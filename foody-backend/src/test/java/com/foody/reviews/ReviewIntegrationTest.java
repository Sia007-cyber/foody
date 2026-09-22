package com.foody.reviews;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.foody.AbstractContainerBaseTest;
import com.foody.businesses.entity.*;
import com.foody.businesses.repository.BusinessRepository;
import com.foody.common.exception.*;
import com.foody.reviews.dto.*;
import com.foody.reviews.entity.Review;
import com.foody.reviews.repository.ReviewRepository;
import com.foody.reviews.service.ReviewService;
import com.foody.reviews.service.ReviewModerationService;
import com.foody.reviews.entity.ReviewModerationStatus;
import com.foody.users.entity.*;
import com.foody.users.repository.UserRepository;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;

@AutoConfigureMockMvc
class ReviewIntegrationTest extends AbstractContainerBaseTest {
    @Autowired ReviewService service;
    @Autowired ReviewRepository reviews;
    @Autowired ReviewModerationService moderation;
    @Autowired UserRepository users;
    @Autowired BusinessRepository businesses;
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;

    User customer;
    User otherCustomer;
    User otherOwner;
    Business approved;

    @BeforeEach
    void setUp() {
        User owner = user(UserRole.BUSINESS_OWNER, "Review Owner");
        customer = user(UserRole.CUSTOMER, "Alice Reviewer");
        otherCustomer = user(UserRole.CUSTOMER, "Bob Reviewer");
        otherOwner = user(UserRole.BUSINESS_OWNER, "Other Owner Reviewer");
        approved = business(owner, BusinessStatus.APPROVED);
    }

    @Test
    void newReviewIsPendingAndNotPublicUntilApproved() {
        ReviewResponse created = service.create(approved.getId(), customer.getId(), new ReviewRequest(5, "  Excellent  "));
        assertThat(created.comment()).isEqualTo("Excellent");
        assertThat(created.reviewerDisplayName()).isEqualTo("Alice Reviewer");
        assertThat(created.moderationStatus()).isEqualTo(ReviewModerationStatus.PENDING);
        assertThat(service.list(approved.getId()).reviews()).isEmpty();

        moderation.moderate("BUSINESS", created.id(), ReviewModerationStatus.APPROVED);
        ReviewListResponse listed = service.list(approved.getId());
        assertThat(listed.reviews()).extracting(ReviewResponse::id).containsExactly(created.id());
        assertThat(listed.averageRating()).isEqualByComparingTo("5.00");
        assertThat(listed.reviewCount()).isEqualTo(1);
    }

    @Test
    void businessOwnerCanReviewAnotherBusinessButNotOwnBusiness() {
        assertThat(service.create(approved.getId(), otherOwner.getId(), new ReviewRequest(5, "Good")).rating())
                .isEqualTo(5);
        assertThatThrownBy(() -> service.create(approved.getId(), approved.getOwnerUserId(), new ReviewRequest(5, null)))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void averageAndCountUsePersistedRatings() {
        ReviewResponse first=service.create(approved.getId(), customer.getId(), new ReviewRequest(5, null));
        ReviewResponse second=service.create(approved.getId(), otherCustomer.getId(), new ReviewRequest(2, null));
        moderation.moderate("BUSINESS",first.id(),ReviewModerationStatus.APPROVED);
        moderation.moderate("BUSINESS",second.id(),ReviewModerationStatus.APPROVED);

        ReviewListResponse result = service.list(approved.getId());
        assertThat(result.averageRating()).isEqualByComparingTo("3.50");
        assertThat(result.reviewCount()).isEqualTo(2);
    }

    @Test
    void ratingsOutsideOneToFiveAreRejected() {
        assertThatThrownBy(() -> service.create(approved.getId(), customer.getId(), new ReviewRequest(0, null)))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.create(approved.getId(), customer.getId(), new ReviewRequest(6, null)))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void duplicateReviewIsRejectedAndDatabaseConstraintIsAuthoritative() {
        service.create(approved.getId(), customer.getId(), new ReviewRequest(4, null));
        assertThatThrownBy(() -> service.create(approved.getId(), customer.getId(), new ReviewRequest(5, null)))
                .isInstanceOf(DuplicateResourceException.class);

        Review duplicate = review(approved.getId(), customer.getId(), 3);
        assertThatThrownBy(() -> reviews.saveAndFlush(duplicate)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void concurrentCreatesProduceOneReviewAndOneConflict() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> task = () -> {
            start.await();
            try {
                service.create(approved.getId(), customer.getId(), new ReviewRequest(4, null));
                return true;
            } catch (DuplicateResourceException ex) {
                return false;
            }
        };
        Future<Boolean> first = pool.submit(task);
        Future<Boolean> second = pool.submit(task);
        start.countDown();
        assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(true, false);
        pool.shutdownNow();
        assertThat(reviews.findById(service.mine(approved.getId(),customer.getId()).id())).isPresent();
    }

    @Test
    void customerUpdatesOnlyOwnReviewAndCreatedAtIsPreserved() {
        ReviewResponse original = service.create(approved.getId(), customer.getId(), new ReviewRequest(2, "old"));
        ReviewResponse updated = service.update(approved.getId(), customer.getId(), new ReviewRequest(5, "   "));

        assertThat(updated.id()).isEqualTo(original.id());
        assertThat(updated.createdAt()).isEqualTo(original.createdAt());
        assertThat(updated.updatedAt()).isAfterOrEqualTo(original.updatedAt());
        assertThat(updated.rating()).isEqualTo(5);
        assertThat(updated.comment()).isNull();
        assertThat(updated.moderationStatus()).isEqualTo(ReviewModerationStatus.PENDING);
        assertThatThrownBy(() -> service.update(approved.getId(), otherCustomer.getId(), new ReviewRequest(1, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void customerDeletesOnlyOwnReview() {
        service.create(approved.getId(), customer.getId(), new ReviewRequest(4, null));
        assertThatThrownBy(() -> service.delete(approved.getId(), otherCustomer.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
        service.delete(approved.getId(), customer.getId());
        assertThat(reviews.findByBusinessIdAndCustomerUserId(approved.getId(),customer.getId())).isEmpty();
    }

    @Test
    void nonApprovedBusinessCannotBeReviewedOrListed() {
        Business pending = business(user(UserRole.BUSINESS_OWNER, "Pending Owner"), BusinessStatus.PENDING);
        assertThatThrownBy(() -> service.create(pending.getId(), customer.getId(), new ReviewRequest(4, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.list(pending.getId())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void emptyListHasTruthfulZeroSummaryAndResponseHasNoPrivateUserData() {
        ReviewListResponse result = service.list(approved.getId());
        assertThat(result.reviews()).isEmpty();
        assertThat(result.averageRating()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.reviewCount()).isZero();
        assertThat(Arrays.stream(ReviewResponse.class.getRecordComponents()).map(RecordComponent::getName))
                .doesNotContain("customerUserId", "email", "phone", "passwordHash");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCannotUseCustomerWriteEndpoint() throws Exception {
        mockMvc.perform(post("/api/businesses/{id}/reviews", approved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5,\"comment\":\"No\"}"))
                .andExpect(status().isForbidden());
    }

    @Test void rejectedReviewRemainsNonPublic(){ReviewResponse created=service.create(approved.getId(),customer.getId(),new ReviewRequest(3,"No"));moderation.moderate("BUSINESS",created.id(),ReviewModerationStatus.REJECTED);assertThat(service.list(approved.getId()).reviews()).isEmpty();assertThat(service.mine(approved.getId(),customer.getId()).moderationStatus()).isEqualTo(ReviewModerationStatus.REJECTED);}

    @Test @WithMockUser(roles="CUSTOMER") void nonAdminCannotModerateReview() throws Exception {ReviewResponse created=service.create(approved.getId(),customer.getId(),new ReviewRequest(4,null));mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/admin/reviews/business/{id}/approve",created.id())).andExpect(status().isForbidden());}

    @Test @WithMockUser(roles="ADMIN") void adminCanApproveAndRejectThroughPersistedEndpoints() throws Exception {ReviewResponse first=service.create(approved.getId(),customer.getId(),new ReviewRequest(4,null));mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/admin/reviews/business/{id}/approve",first.id())).andExpect(status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.moderationStatus").value("APPROVED"));ReviewResponse second=service.create(approved.getId(),otherCustomer.getId(),new ReviewRequest(2,null));mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/admin/reviews/business/{id}/reject",second.id())).andExpect(status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.moderationStatus").value("REJECTED"));}

    @Test void migrationDefaultsLegacyRowsToApproved(){String businessDefault=jdbc.queryForObject("select column_default from information_schema.columns where table_schema=database() and table_name='reviews' and column_name='moderation_status'",String.class);String productDefault=jdbc.queryForObject("select column_default from information_schema.columns where table_schema=database() and table_name='product_reviews' and column_name='moderation_status'",String.class);assertThat(businessDefault).isEqualTo("APPROVED");assertThat(productDefault).isEqualTo("APPROVED");}

    private User user(UserRole role, String name) {
        User user = new User();
        user.setEmail(UUID.randomUUID() + "@reviews.test");
        user.setFullName(name);
        user.setPasswordHash("unused");
        user.setRole(role);
        return users.saveAndFlush(user);
    }

    private Business business(User owner, BusinessStatus status) {
        Business business = new Business();
        business.setOwnerUserId(owner.getId());
        business.setName("Review Cafe " + UUID.randomUUID());
        business.setBusinessType("CAFE");
        business.setStatus(status);
        return businesses.saveAndFlush(business);
    }

    private static Review review(Long businessId, Long customerId, int rating) {
        Review review = new Review();
        review.setBusinessId(businessId);
        review.setCustomerUserId(customerId);
        review.setRating(rating);
        return review;
    }
}
