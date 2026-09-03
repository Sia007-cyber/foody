package com.foody.common.storage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.foody.common.exception.GlobalExceptionHandler;
import com.foody.common.exception.InvalidRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class UploadControllerTest {

    @Mock FileStorageService fileStorageService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new UploadController(fileStorageService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void uploadImage_returnsStoredUrl() throws Exception {
        when(fileStorageService.storeImage(any())).thenReturn("/uploads/generated-name.jpg");
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/uploads/image").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/uploads/generated-name.jpg"));
    }

    @Test
    void uploadImage_propagatesValidationErrorAsBadRequest() throws Exception {
        when(fileStorageService.storeImage(any())).thenThrow(new InvalidRequestException("فقط تصاویر مجاز هستن"));
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1});

        mockMvc.perform(multipart("/api/uploads/image").file(file))
                .andExpect(status().isBadRequest());
    }
}
