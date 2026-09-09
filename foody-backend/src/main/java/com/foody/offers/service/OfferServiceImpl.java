package com.foody.offers.service;

import com.foody.businesses.entity.*;
import com.foody.businesses.repository.BusinessRepository;
import com.foody.businesses.service.CustomerBusinessAccessPolicy;
import com.foody.common.exception.*;
import com.foody.offers.dto.*;
import com.foody.offers.entity.*;
import com.foody.offers.repository.*;
import java.time.*;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class OfferServiceImpl implements OfferService {
    private final OfferRepository offers; private final OfferClaimRepository claims; private final BusinessRepository businesses;
    OfferServiceImpl(OfferRepository offers, OfferClaimRepository claims, BusinessRepository businesses){this.offers=offers;this.claims=claims;this.businesses=businesses;}

    @Override @Transactional public OfferResponse create(Long ownerUserId, CreateOfferRequest r){
        Business b=ownedBusiness(ownerUserId);
        if(r.capacity()<=0) throw new InvalidRequestException("Offer capacity must be greater than zero");
        if(!r.expiresAt().isAfter(r.startsAt())) throw new InvalidRequestException("Offer expiry must be after its start");
        Offer o=new Offer(); o.setBusinessId(b.getId()); o.setTitle(r.title().trim()); o.setDescription(r.description()); o.setCapacity(r.capacity()); o.setStartsAt(r.startsAt()); o.setExpiresAt(r.expiresAt());
        return OfferResponse.from(offers.save(o),b.getName(),0);
    }
    @Override @Transactional(readOnly=true) public List<OfferResponse> ownerOffers(Long ownerUserId){
        Business b=ownedBusiness(ownerUserId); return offers.findByBusinessIdOrderByCreatedAtDesc(b.getId()).stream().map(o->response(o,b.getName())).toList();
    }
    @Override @Transactional public OfferResponse cancel(Long ownerUserId,Long id){
        Business b=ownedBusiness(ownerUserId); Offer o=offers.findByIdAndBusinessId(id,b.getId()).orElseThrow(()->new ResourceNotFoundException("Offer not found: "+id));
        if(o.getStatus()==OfferStatus.CANCELLED) throw new InvalidStateTransitionException("Offer is already cancelled");
        o.setStatus(OfferStatus.CANCELLED); return OfferResponse.from(offers.save(o),b.getName(),claims.countByOfferId(id));
    }
    @Override @Transactional(readOnly=true) public List<OfferResponse> claimableOffers(){
        return offers.findClaimable(Instant.now()).stream().map(o->response(o,businesses.findById(o.getBusinessId()).orElseThrow().getName())).toList();
    }
    @Override @Transactional public OfferClaimResponse claim(Long customerUserId,Long offerId){
        Offer o=offers.findByIdForUpdate(offerId).orElseThrow(()->new ResourceNotFoundException("Offer not found: "+offerId));
        Business b=businesses.findById(o.getBusinessId()).orElseThrow(()->new ResourceNotFoundException("Business not found"));
        if(b.getStatus()!=BusinessStatus.APPROVED) throw new ResourceNotFoundException("Offer not found: "+offerId);
        CustomerBusinessAccessPolicy.requireNotOwnedBy(customerUserId,b);
        Instant now=Instant.now();
        if(o.getStatus()!=OfferStatus.ACTIVE) throw new InvalidStateTransitionException("Offer is cancelled");
        if(now.isBefore(o.getStartsAt())) throw new InvalidStateTransitionException("Offer has not started");
        if(!now.isBefore(o.getExpiresAt())) throw new InvalidStateTransitionException("Offer has expired");
        if(claims.existsByOfferIdAndCustomerUserId(offerId,customerUserId)) throw new DuplicateResourceException("You have already claimed this offer");
        long count=claims.countByOfferId(offerId); if(count>=o.getCapacity()) throw new InvalidStateTransitionException("Offer capacity is exhausted");
        OfferClaim c=new OfferClaim(); c.setOfferId(offerId); c.setCustomerUserId(customerUserId);
        try { c=claims.saveAndFlush(c); } catch(DataIntegrityViolationException e){ throw new DuplicateResourceException("You have already claimed this offer"); }
        return OfferClaimResponse.from(c,o.getCapacity()-count-1);
    }
    @Override @Transactional(readOnly=true) public List<OfferClaimResponse> myClaims(Long customerUserId){
        return claims.findByCustomerUserIdOrderByClaimedAtDesc(customerUserId).stream().map(c->{Offer o=offers.findById(c.getOfferId()).orElseThrow(); return OfferClaimResponse.from(c,Math.max(0,o.getCapacity()-claims.countByOfferId(o.getId())));}).toList();
    }
    private Business ownedBusiness(Long userId){return businesses.findByOwnerUserId(userId).orElseThrow(()->new ResourceNotFoundException("No business found for this owner"));}
    private OfferResponse response(Offer o,String businessName){return OfferResponse.from(o,businessName,claims.countByOfferId(o.getId()));}
}
