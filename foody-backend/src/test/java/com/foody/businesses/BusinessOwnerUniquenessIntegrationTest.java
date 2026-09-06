package com.foody.businesses;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.foody.AbstractContainerBaseTest;
import com.foody.businesses.dto.CreateBusinessRequest;
import com.foody.businesses.entity.Business;
import com.foody.businesses.repository.BusinessRepository;
import com.foody.businesses.service.BusinessService;
import com.foody.common.exception.DuplicateResourceException;
import com.foody.users.entity.User;
import com.foody.users.entity.UserRole;
import com.foody.users.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

class BusinessOwnerUniquenessIntegrationTest extends AbstractContainerBaseTest {

    @Autowired BusinessService businessService;
    @Autowired BusinessRepository businessRepository;
    @Autowired UserRepository userRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void firstBusinessRegistrationSucceeds() {
        User owner = createOwner();

        Business business = businessService.createForOwner(owner.getId(), request("First business"));

        assertThat(business.getId()).isNotNull();
        assertThat(businessRepository.findByOwnerUserId(owner.getId()))
                .map(Business::getId)
                .contains(business.getId());
    }

    @Test
    void secondBusinessRegistrationForSameOwnerIsRejected() {
        User owner = createOwner();
        businessService.createForOwner(owner.getId(), request("First business"));

        assertThatThrownBy(() -> businessService.createForOwner(owner.getId(), request("Second business")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("This owner already has a business");
    }

    @Test
    void databaseUniqueConstraintPreventsDuplicateOwnerRows() {
        User owner = createOwner();
        businessService.createForOwner(owner.getId(), request("First business"));

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO businesses (owner_user_id, name, business_type, status)
                VALUES (?, ?, 'CAFE', 'PENDING')
                """, owner.getId(), "Direct duplicate"))
                .isInstanceOf(DuplicateKeyException.class);
    }

    private User createOwner() {
        User owner = new User();
        owner.setEmail("business-owner-" + UUID.randomUUID() + "@foody.test");
        owner.setFullName("Business owner");
        owner.setPasswordHash("not-used-by-this-test");
        owner.setRole(UserRole.BUSINESS_OWNER);
        return userRepository.saveAndFlush(owner);
    }

    private CreateBusinessRequest request(String name) {
        return new CreateBusinessRequest(name, "CAFE", null, null, null);
    }
}
