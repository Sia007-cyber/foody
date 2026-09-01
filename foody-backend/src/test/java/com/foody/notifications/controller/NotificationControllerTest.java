package com.foody.notifications.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.foody.auth.security.FoodyUserPrincipal;
import com.foody.common.exception.GlobalExceptionHandler;
import com.foody.common.exception.ResourceNotFoundException;
import com.foody.notifications.dto.NotificationResponse;
import com.foody.notifications.entity.NotificationType;
import com.foody.notifications.service.NotificationService;
import com.foody.users.entity.User;
import com.foody.users.entity.UserRole;
import com.foody.users.entity.UserStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    static final Long USER_ID = 1L;

    @Mock NotificationService notificationService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        NotificationController controller = new NotificationController(notificationService);

        User user = new User();
        user.setId(USER_ID);
        user.setEmail("customer@foody.test");
        user.setFullName("Test Customer");
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);
        FoodyUserPrincipal principal = new FoodyUserPrincipal(user);

        HandlerMethodArgumentResolver principalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().equals(FoodyUserPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return principal;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(principalResolver)
                .build();
    }

    private NotificationResponse sample(Long id, boolean read) {
        return new NotificationResponse(
                id, NotificationType.ORDER_STATUS_CHANGED, "به‌روزرسانی سفارش",
                "وضعیت سفارش تغییر کرد.", "ORDER", 100L, read, Instant.now());
    }

    @Test
    void myNotifications_returnsCallersNotifications() throws Exception {
        when(notificationService.getMyNotifications(USER_ID))
                .thenReturn(List.of(sample(1L, false)));

        mockMvc.perform(get("/api/notifications/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].read").value(false));
    }

    @Test
    void unreadCount_returnsCount() throws Exception {
        when(notificationService.getUnreadCount(USER_ID)).thenReturn(4L);

        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(4));
    }

    @Test
    void markAsRead_returnsUpdatedNotification() throws Exception {
        when(notificationService.markAsRead(eq(1L), eq(USER_ID))).thenReturn(sample(1L, true));

        mockMvc.perform(patch("/api/notifications/1/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));
    }

    @Test
    void markAsRead_returns404WhenNotOwned() throws Exception {
        when(notificationService.markAsRead(eq(999L), eq(USER_ID)))
                .thenThrow(new ResourceNotFoundException("Notification not found: 999"));

        mockMvc.perform(patch("/api/notifications/999/read"))
                .andExpect(status().isNotFound());
    }

    @Test
    void markAllAsRead_returnsOk() throws Exception {
        mockMvc.perform(patch("/api/notifications/read-all"))
                .andExpect(status().isOk());
    }
}
