package com.foody.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import jakarta.persistence.OptimisticLockException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void optimisticLockFailure_returnsSafeConflictEnvelope() {
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/business/orders/42/status");
        OptimisticLockingFailureException failure =
                new OptimisticLockingFailureException("internal persistence detail");

        ResponseEntity<ErrorResponse> response = handler.handleOptimisticLock(failure, request);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("CONCURRENT_MODIFICATION");
        assertThat(response.getBody().message())
                .isEqualTo("The resource was modified by another request. Please reload and try again.")
                .doesNotContain("persistence");
        assertThat(response.getBody().path()).isEqualTo("/api/business/orders/42/status");
    }

    @Test
    void jpaOptimisticLockFailure_returnsTheSameConflictEnvelope() {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "PATCH", "/api/business/reservations/42/status");

        ResponseEntity<ErrorResponse> response = handler.handleOptimisticLock(
                new OptimisticLockException("internal Hibernate detail"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("CONCURRENT_MODIFICATION");
        assertThat(response.getBody().message()).doesNotContain("Hibernate");
    }

    @Test
    void databaseUniquenessFailure_returnsConflictEnvelope() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/business");

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("Duplicate entry for uk_businesses_owner_user_id"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("RESOURCE_ALREADY_EXISTS");
        assertThat(response.getBody().message()).doesNotContain("uk_businesses");
        assertThat(response.getBody().path()).isEqualTo("/api/business");
    }
}
