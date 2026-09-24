package com.foody.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import com.foody.AbstractContainerBaseTest;
import com.foody.businesses.entity.Business;
import com.foody.businesses.entity.BusinessStatus;
import com.foody.businesses.repository.BusinessRepository;
import com.foody.businesses.service.BusinessService;
import com.foody.menus.entity.Menu;
import com.foody.menus.repository.MenuRepository;
import com.foody.products.dto.ProductDiscoveryResponse;
import com.foody.products.entity.Product;
import com.foody.products.repository.ProductRepository;
import com.foody.products.service.ProductService;
import com.foody.reviews.entity.ProductReview;
import com.foody.reviews.entity.Review;
import com.foody.reviews.entity.ReviewModerationStatus;
import com.foody.reviews.repository.ProductReviewRepository;
import com.foody.reviews.repository.ReviewRepository;
import com.foody.users.entity.User;
import com.foody.users.entity.UserRole;
import com.foody.users.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class DiscoveryIntegrationTest extends AbstractContainerBaseTest {

    @Autowired BusinessService businessService;
    @Autowired ProductService productService;
    @Autowired BusinessRepository businesses;
    @Autowired MenuRepository menus;
    @Autowired ProductRepository products;
    @Autowired ReviewRepository reviews;
    @Autowired ProductReviewRepository productReviews;
    @Autowired UserRepository users;

    User customer;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setEmail(UUID.randomUUID() + "@discovery.test");
        customer.setFullName("Discovery customer");
        customer.setPasswordHash("unused");
        customer.setRole(UserRole.CUSTOMER);
        customer = users.saveAndFlush(customer);
    }

    @Test
    void businessSearchSupportsPartialPersianVariantsAndExcludesNonPublicBusinesses() {
        Business publicBusiness = business("کباب سرای ساحل", BusinessStatus.APPROVED, false);
        business("کباب سرای پنهان", BusinessStatus.SUSPENDED, false);

        List<Business> results = businessService.search(null, "  كباب  ");

        assertThat(results).extracting(Business::getId).contains(publicBusiness.getId());
        assertThat(results).allMatch(b -> b.getStatus() == BusinessStatus.APPROVED);
    }

    @Test
    void topRatedBusinessesUseOnlyApprovedReviewsWithDeterministicRatedFirstOrdering() {
        Business high = business("A high " + UUID.randomUUID(), BusinessStatus.APPROVED, false);
        Business low = business("B low " + UUID.randomUUID(), BusinessStatus.APPROVED, false);
        Business pendingOnly = business("C pending " + UUID.randomUUID(), BusinessStatus.APPROVED, false);
        review(high, 5, ReviewModerationStatus.APPROVED);
        review(low, 2, ReviewModerationStatus.APPROVED);
        review(pendingOnly, 5, ReviewModerationStatus.PENDING);

        List<Business> ranked = businessService.findTopRated();

        assertThat(ranked.indexOf(high)).isLessThan(ranked.indexOf(low));
        assertThat(ranked.indexOf(low)).isLessThan(ranked.indexOf(pendingOnly));
    }

    @Test
    void featuredBusinessesRemainAdminCuratedAndPublicOnly() {
        Business featured = business("Featured " + UUID.randomUUID(), BusinessStatus.APPROVED, true);
        business("Hidden featured " + UUID.randomUUID(), BusinessStatus.SUSPENDED, true);

        assertThat(businessService.findFeatured()).extracting(Business::getId).contains(featured.getId());
        assertThat(businessService.findFeatured()).allMatch(b -> b.getStatus() == BusinessStatus.APPROVED && b.isFeatured());
    }

    @Test
    void topProductsUsePersistedApprovedRatingsAndExcludeUnavailableOrNonPublicProducts() {
        Business publicBusiness = business("Public " + UUID.randomUUID(), BusinessStatus.APPROVED, false);
        Business hiddenBusiness = business("Hidden " + UUID.randomUUID(), BusinessStatus.SUSPENDED, false);
        Product best = product(publicBusiness, "Best " + UUID.randomUUID(), true);
        Product lower = product(publicBusiness, "Lower " + UUID.randomUUID(), true);
        Product unavailable = product(publicBusiness, "Unavailable " + UUID.randomUUID(), false);
        Product hidden = product(hiddenBusiness, "Hidden " + UUID.randomUUID(), true);
        productReview(best, 5, ReviewModerationStatus.APPROVED);
        productReview(lower, 3, ReviewModerationStatus.APPROVED);
        productReview(unavailable, 5, ReviewModerationStatus.APPROVED);
        productReview(hidden, 5, ReviewModerationStatus.APPROVED);

        List<ProductDiscoveryResponse> ranked = products.findTopRatedPublic(PageRequest.of(0, 100));

        assertThat(ids(ranked)).contains(best.getId(), lower.getId()).doesNotContain(unavailable.getId(), hidden.getId());
        assertThat(ids(ranked).indexOf(best.getId())).isLessThan(ids(ranked).indexOf(lower.getId()));
        assertThat(ranked.stream().filter(item -> item.id().equals(best.getId())).findFirst().orElseThrow().averageRating()).isEqualTo(5.0);
    }

    @Test
    void productSearchSupportsPartialPersianVariantsAndExcludesIneligibleProducts() {
        Business publicBusiness = business("Public " + UUID.randomUUID(), BusinessStatus.APPROVED, false);
        Business hiddenBusiness = business("Hidden " + UUID.randomUUID(), BusinessStatus.SUSPENDED, false);
        Product match = product(publicBusiness, "چلو كباب مخصوص", true);
        Product unavailable = product(publicBusiness, "چلو کباب ناموجود", false);
        Product hidden = product(hiddenBusiness, "چلو کباب پنهان", true);

        List<ProductDiscoveryResponse> results = productService.searchPublic("  کباب  ");

        assertThat(ids(results)).contains(match.getId()).doesNotContain(unavailable.getId(), hidden.getId());
    }

    private List<Long> ids(List<ProductDiscoveryResponse> items) {
        return items.stream().map(ProductDiscoveryResponse::id).toList();
    }

    private Business business(String name, BusinessStatus status, boolean featured) {
        User owner = new User();
        owner.setEmail(UUID.randomUUID() + "@discovery-owner.test");
        owner.setFullName("Discovery owner");
        owner.setPasswordHash("unused");
        owner.setRole(UserRole.BUSINESS_OWNER);
        owner = users.saveAndFlush(owner);
        Business business = new Business();
        business.setOwnerUserId(owner.getId());
        business.setName(name);
        business.setBusinessType("CAFE");
        business.setStatus(status);
        business.setFeatured(featured);
        return businesses.saveAndFlush(business);
    }

    private Product product(Business business, String name, boolean available) {
        Menu menu = new Menu();
        menu.setBusinessId(business.getId());
        menu.setName("Menu " + UUID.randomUUID());
        menu = menus.saveAndFlush(menu);
        Product product = new Product();
        product.setMenuId(menu.getId());
        product.setName(name);
        product.setPrice(BigDecimal.TEN);
        product.setIsAvailable(available);
        return products.saveAndFlush(product);
    }

    private void review(Business business, int rating, ReviewModerationStatus status) {
        Review review = new Review();
        review.setBusinessId(business.getId());
        review.setCustomerUserId(customer.getId());
        review.setRating(rating);
        review.setModerationStatus(status);
        reviews.saveAndFlush(review);
    }

    private void productReview(Product product, int rating, ReviewModerationStatus status) {
        ProductReview review = new ProductReview();
        review.setProductId(product.getId());
        review.setReviewerUserId(customer.getId());
        review.setRating(rating);
        review.setModerationStatus(status);
        productReviews.saveAndFlush(review);
    }
}
