package com.foody.offers;

import static org.assertj.core.api.Assertions.*;
import com.foody.AbstractContainerBaseTest;
import com.foody.businesses.entity.*;
import com.foody.businesses.repository.BusinessRepository;
import com.foody.common.exception.*;
import com.foody.offers.dto.*;
import com.foody.offers.entity.*;
import com.foody.offers.repository.*;
import com.foody.offers.service.OfferService;
import com.foody.users.entity.*;
import com.foody.users.repository.UserRepository;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

class OfferIntegrationTest extends AbstractContainerBaseTest {
    @Autowired OfferService service; @Autowired UserRepository users; @Autowired BusinessRepository businesses;
    @Autowired OfferRepository offers; @Autowired OfferClaimRepository claims;
    User owner,otherOwner,customer,otherCustomer; Business cafe,otherCafe;
    @BeforeEach void setup(){owner=user(UserRole.BUSINESS_OWNER);otherOwner=user(UserRole.BUSINESS_OWNER);customer=user(UserRole.CUSTOMER);otherCustomer=user(UserRole.CUSTOMER);cafe=business(owner,BusinessStatus.APPROVED);otherCafe=business(otherOwner,BusinessStatus.APPROVED);}

    @Test void ownerCreatesOfferForOwnDerivedBusiness(){OfferResponse r=service.create(owner.getId(),request(3,Instant.now().minusSeconds(2),Instant.now().plusSeconds(600)));assertThat(r.businessId()).isEqualTo(cafe.getId());assertThat(r.claimCount()).isZero();}
    @Test void invalidCapacityAndTimeRangeAreRejected(){assertThatThrownBy(()->service.create(owner.getId(),request(1,Instant.now(),Instant.now().minusSeconds(1)))).isInstanceOf(InvalidRequestException.class);assertThatThrownBy(()->service.create(owner.getId(),request(0,Instant.now(),Instant.now().plusSeconds(1)))).isInstanceOf(InvalidRequestException.class);}
    @Test void ownerCannotOperateOnAnotherBusinessOffer(){OfferResponse o=create(otherOwner,2);assertThatThrownBy(()->service.cancel(owner.getId(),o.id())).isInstanceOf(ResourceNotFoundException.class);}
    @Test void customerClaimsActiveOfferOnce(){OfferResponse o=create(owner,2);OfferClaimResponse c=service.claim(customer.getId(),o.id());assertThat(c.offerId()).isEqualTo(o.id());assertThat(c.remainingAvailability()).isEqualTo(1);}
    @Test void duplicateClaimIsRejected(){OfferResponse o=create(owner,2);service.claim(customer.getId(),o.id());assertThatThrownBy(()->service.claim(customer.getId(),o.id())).isInstanceOf(DuplicateResourceException.class);}
    @Test void inactiveTimeAndCancelledOffersAreRejected(){Instant now=Instant.now();OfferResponse future=service.create(owner.getId(),request(1,now.plusSeconds(60),now.plusSeconds(120)));assertThatThrownBy(()->service.claim(customer.getId(),future.id())).isInstanceOf(InvalidStateTransitionException.class);OfferResponse expired=service.create(owner.getId(),request(1,now.minusSeconds(120),now.minusSeconds(60)));assertThatThrownBy(()->service.claim(customer.getId(),expired.id())).isInstanceOf(InvalidStateTransitionException.class);OfferResponse cancelled=create(owner,1);service.cancel(owner.getId(),cancelled.id());assertThatThrownBy(()->service.claim(customer.getId(),cancelled.id())).isInstanceOf(InvalidStateTransitionException.class);}
    @Test void exhaustedOfferIsRejected(){OfferResponse o=create(owner,1);service.claim(customer.getId(),o.id());assertThatThrownBy(()->service.claim(otherCustomer.getId(),o.id())).isInstanceOf(InvalidStateTransitionException.class);}
    @Test void concurrentClaimsCannotExceedCapacity() throws Exception {OfferResponse o=create(owner,1);ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);Callable<Boolean> task1=()->attempt(start,customer.getId(),o.id()),task2=()->attempt(start,otherCustomer.getId(),o.id());Future<Boolean>a=pool.submit(task1),b=pool.submit(task2);start.countDown();assertThat(List.of(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);pool.shutdownNow();assertThat(claims.countByOfferId(o.id())).isEqualTo(1);}
    @Test void publicListOnlyIncludesClaimableOffersOfApprovedBusinesses(){OfferResponse visible=create(owner,2);User pendingOwner=user(UserRole.BUSINESS_OWNER);business(pendingOwner,BusinessStatus.PENDING);OfferResponse hidden=create(pendingOwner,2);assertThat(service.claimableOffers()).extracting(OfferResponse::id).contains(visible.id()).doesNotContain(hidden.id());}
    @Test void ownerSeesAccurateCountWithoutPrivateCustomerData(){OfferResponse o=create(owner,2);service.claim(customer.getId(),o.id());OfferResponse result=service.ownerOffers(owner.getId()).stream().filter(x->x.id().equals(o.id())).findFirst().orElseThrow();assertThat(result.claimCount()).isEqualTo(1);assertThat(Arrays.stream(OfferResponse.class.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName)).doesNotContain("customerUserId","customerEmail","customerName");}

    private boolean attempt(CountDownLatch start,Long customerId,Long offerId)throws InterruptedException{start.await();try{service.claim(customerId,offerId);return true;}catch(InvalidStateTransitionException|DuplicateResourceException e){return false;}}
    private OfferResponse create(User o,int capacity){return service.create(o.getId(),request(capacity,Instant.now().minusSeconds(5),Instant.now().plusSeconds(600)));}
    private CreateOfferRequest request(int capacity,Instant start,Instant end){return new CreateOfferRequest("Limited offer",null,capacity,start,end);}
    private User user(UserRole role){User u=new User();u.setEmail(UUID.randomUUID()+"@offer.test");u.setFullName("Offer test");u.setPasswordHash("unused");u.setRole(role);return users.saveAndFlush(u);}
    private Business business(User owner,BusinessStatus status){Business b=new Business();b.setOwnerUserId(owner.getId());b.setName("Cafe "+UUID.randomUUID());b.setBusinessType("CAFE");b.setStatus(status);return businesses.saveAndFlush(b);}
}
