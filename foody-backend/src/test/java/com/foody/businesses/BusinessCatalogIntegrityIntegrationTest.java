package com.foody.businesses;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.foody.AbstractContainerBaseTest;
import com.foody.businesses.dto.CreateBusinessRequest;
import com.foody.businesses.entity.Business;
import com.foody.businesses.entity.BusinessStatus;
import com.foody.businesses.repository.BusinessRepository;
import com.foody.businesses.service.BusinessService;
import com.foody.menus.dto.CreateMenuRequest;
import com.foody.menus.entity.Menu;
import com.foody.menus.service.MenuService;
import com.foody.products.dto.CreateProductRequest;
import com.foody.products.entity.Product;
import com.foody.products.service.ProductService;
import com.foody.users.entity.User;
import com.foody.users.entity.UserRole;
import com.foody.users.repository.UserRepository;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.transaction.support.TransactionTemplate;

class BusinessCatalogIntegrityIntegrationTest extends AbstractContainerBaseTest {

    @Autowired BusinessService businessService;
    @Autowired BusinessRepository businessRepository;
    @Autowired MenuService menuService;
    @Autowired ProductService productService;
    @Autowired UserRepository userRepository;
    @Autowired TransactionTemplate transactions;
    @Autowired JdbcTemplate jdbc;

    @Test
    void staleModerationDecisionCannotOverwriteNewerDecision() {
        User owner = user(UserRole.BUSINESS_OWNER);
        Business created = businessService.createForOwner(owner.getId(),
                new CreateBusinessRequest("Concurrency cafe", "CAFE", null, null, null));

        Business approveDecision = transactions.execute(status -> businessRepository.findById(created.getId()).orElseThrow());
        Business rejectDecision = transactions.execute(status -> businessRepository.findById(created.getId()).orElseThrow());

        approveDecision.setStatus(BusinessStatus.APPROVED);
        transactions.executeWithoutResult(status -> businessRepository.saveAndFlush(approveDecision));

        rejectDecision.setStatus(BusinessStatus.REJECTED);
        assertThatThrownBy(() -> transactions.executeWithoutResult(
                status -> businessRepository.saveAndFlush(rejectDecision)))
                .isInstanceOf(OptimisticLockingFailureException.class);

        assertThat(businessRepository.findById(created.getId()).orElseThrow().getStatus())
                .isEqualTo(BusinessStatus.APPROVED);
    }

    @Test
    void deletingCatalogPreservesHistoricalOrderItemSnapshot() {
        User owner = user(UserRole.BUSINESS_OWNER);
        User customer = user(UserRole.CUSTOMER);
        Business business = businessService.createForOwner(owner.getId(),
                new CreateBusinessRequest("History cafe", "CAFE", null, null, null));
        Menu menu = menuService.createMenu(owner.getId(), new CreateMenuRequest("Main", 0));
        Product product = productService.createProduct(owner.getId(), new CreateProductRequest(
                menu.getId(), "Archived latte", null, new BigDecimal("4.50"), null, 0));
        long orderId = insertOrder(customer.getId(), business.getId());
        jdbc.update("""
                INSERT INTO order_items
                    (order_id, product_id, product_name, unit_price, quantity, subtotal)
                VALUES (?, ?, 'Archived latte', 4.50, 2, 9.00)
                """, orderId, product.getId());

        menuService.deleteMenu(owner.getId(), menu.getId());

        assertThat(productService.findById(product.getId())).isEmpty();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM order_items WHERE order_id = ?", Long.class, orderId)).isEqualTo(1L);
        assertThat(jdbc.queryForObject(
                "SELECT product_id FROM order_items WHERE order_id = ?", Long.class, orderId)).isNull();
        assertThat(jdbc.queryForObject(
                "SELECT product_name FROM order_items WHERE order_id = ?", String.class, orderId))
                .isEqualTo("Archived latte");
    }

    private User user(UserRole role) {
        User user = new User();
        user.setEmail(role.name().toLowerCase() + "-" + UUID.randomUUID() + "@foody.test");
        user.setFullName("Integrity test user");
        user.setPasswordHash("not-used");
        user.setRole(role);
        return userRepository.saveAndFlush(user);
    }

    private long insertOrder(Long customerId, Long businessId) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO orders
                        (customer_user_id, business_id, fulfillment_type, status, total_amount)
                    VALUES (?, ?, 'PICKUP', 'COMPLETED', 9.00)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, customerId);
            statement.setLong(2, businessId);
            return statement;
        }, keys);
        return keys.getKey().longValue();
    }
}
