package com.foody.businesses.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.foody.businesses.entity.Business;
import com.foody.businesses.entity.BusinessStatus;
import com.foody.businesses.repository.BusinessRepository;
import com.foody.common.exception.InvalidStateTransitionException;
import com.foody.common.exception.ResourceNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BusinessServiceImplTest {

    @Mock BusinessRepository businessRepository;

    BusinessServiceImpl businessService;

    static final Long BUSINESS_ID = 10L;

    @BeforeEach
    void setUp() {
        businessService = new BusinessServiceImpl(businessRepository);
    }

    private Business businessWithStatus(BusinessStatus status) {
        Business business = new Business();
        business.setId(BUSINESS_ID);
        business.setStatus(status);
        return business;
    }

    @Test
    void updateStatus_allowsPendingToApproved() {
        when(businessRepository.findById(BUSINESS_ID)).thenReturn(Optional.of(businessWithStatus(BusinessStatus.PENDING)));
        when(businessRepository.save(any(Business.class))).thenAnswer(inv -> inv.getArgument(0));

        Business result = businessService.updateStatus(BUSINESS_ID, BusinessStatus.APPROVED);

        assertThat(result.getStatus()).isEqualTo(BusinessStatus.APPROVED);
    }

    @Test
    void updateStatus_allowsPendingToRejected() {
        when(businessRepository.findById(BUSINESS_ID)).thenReturn(Optional.of(businessWithStatus(BusinessStatus.PENDING)));
        when(businessRepository.save(any(Business.class))).thenAnswer(inv -> inv.getArgument(0));

        Business result = businessService.updateStatus(BUSINESS_ID, BusinessStatus.REJECTED);

        assertThat(result.getStatus()).isEqualTo(BusinessStatus.REJECTED);
    }

    @Test
    void updateStatus_allowsApprovedToSuspended() {
        when(businessRepository.findById(BUSINESS_ID)).thenReturn(Optional.of(businessWithStatus(BusinessStatus.APPROVED)));
        when(businessRepository.save(any(Business.class))).thenAnswer(inv -> inv.getArgument(0));

        Business result = businessService.updateStatus(BUSINESS_ID, BusinessStatus.SUSPENDED);

        assertThat(result.getStatus()).isEqualTo(BusinessStatus.SUSPENDED);
    }

    @Test
    void updateStatus_rejectsApprovingAlreadyApprovedBusiness() {
        when(businessRepository.findById(BUSINESS_ID)).thenReturn(Optional.of(businessWithStatus(BusinessStatus.APPROVED)));

        assertThatThrownBy(() -> businessService.updateStatus(BUSINESS_ID, BusinessStatus.APPROVED))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void updateStatus_rejectsSuspendingPendingBusiness() {
        when(businessRepository.findById(BUSINESS_ID)).thenReturn(Optional.of(businessWithStatus(BusinessStatus.PENDING)));

        assertThatThrownBy(() -> businessService.updateStatus(BUSINESS_ID, BusinessStatus.SUSPENDED))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void updateStatus_rejectsReactivatingRejectedBusiness() {
        when(businessRepository.findById(BUSINESS_ID)).thenReturn(Optional.of(businessWithStatus(BusinessStatus.REJECTED)));

        assertThatThrownBy(() -> businessService.updateStatus(BUSINESS_ID, BusinessStatus.APPROVED))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void updateStatus_throwsWhenBusinessNotFound() {
        when(businessRepository.findById(BUSINESS_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> businessService.updateStatus(BUSINESS_ID, BusinessStatus.APPROVED))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
