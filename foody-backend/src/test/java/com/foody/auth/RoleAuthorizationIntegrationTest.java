package com.foody.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.foody.AbstractContainerBaseTest;
import com.foody.auth.security.JwtService;
import com.foody.businesses.entity.*;
import com.foody.businesses.repository.BusinessRepository;
import com.foody.reviews.entity.Review;
import com.foody.reviews.repository.ReviewRepository;
import com.foody.users.entity.*;
import com.foody.users.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class RoleAuthorizationIntegrationTest extends AbstractContainerBaseTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired ReviewRepository reviews;
    @Autowired BusinessRepository businesses;
    @Autowired JwtService jwt;

    @Test void adminCanBrowsePublicCatalogButCannotCreateCustomerTransactions() throws Exception {
        String auth = bearer(user(UserRole.ADMIN));
        mvc.perform(get("/api/businesses").header("Authorization", auth)).andExpect(status().isOk());
        mvc.perform(post("/api/orders").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"businessId\":1,\"fulfillmentType\":\"PICKUP\",\"items\":[{\"productId\":1,\"quantity\":1}]}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/reservations").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"businessId\":1,\"date\":\"2030-01-01\",\"time\":\"19:00:00\",\"guestCount\":2}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/offers/1/claim").header("Authorization", auth)).andExpect(status().isForbidden());
        mvc.perform(post("/api/businesses/1/reviews").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"rating\":5}"))
                .andExpect(status().isForbidden());
    }

    @Test void adminCanModerateAnyReviewButCustomerCannotUseAdminModeration() throws Exception {
        User customer = user(UserRole.CUSTOMER);
        Business business = new Business(); business.setOwnerUserId(user(UserRole.BUSINESS_OWNER).getId());
        business.setName("Moderated cafe"); business.setBusinessType("CAFE"); business.setStatus(BusinessStatus.APPROVED);
        business = businesses.saveAndFlush(business);
        Review review = new Review(); review.setBusinessId(business.getId()); review.setCustomerUserId(customer.getId()); review.setRating(1);
        review = reviews.saveAndFlush(review);
        mvc.perform(delete("/api/admin/reviews/{id}", review.getId()).header("Authorization", bearer(customer)))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/admin/reviews/{id}", review.getId()).header("Authorization", bearer(user(UserRole.ADMIN))))
                .andExpect(status().isNoContent());
    }

    private User user(UserRole role) {
        User user = new User(); user.setEmail(UUID.randomUUID()+"@role.test"); user.setFullName("Role test");
        user.setPasswordHash("unused"); user.setRole(role); user.setStatus(UserStatus.ACTIVE); return users.saveAndFlush(user);
    }
    private String bearer(User user) { return "Bearer " + jwt.generateAccessToken(user); }
}
