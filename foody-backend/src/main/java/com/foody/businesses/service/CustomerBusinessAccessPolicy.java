package com.foody.businesses.service;

import com.foody.businesses.entity.Business;
import org.springframework.security.access.AccessDeniedException;
import java.util.Objects;

/** Shared server-side guard against an owner using customer actions on their own business. */
public final class CustomerBusinessAccessPolicy {
    private CustomerBusinessAccessPolicy() {}

    public static void requireNotOwnedBy(Long actorUserId, Business business) {
        if (Objects.equals(business.getOwnerUserId(), actorUserId)) {
            throw new AccessDeniedException("Business owners cannot use customer actions on their own business");
        }
    }
}
