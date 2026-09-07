package com.foody.wallet.dto;

import com.foody.users.entity.User;

/** Deliberately minimal owner-facing customer projection. */
public record CustomerLookupResponse(String publicId, String displayName) {
    public static CustomerLookupResponse from(User user) {
        return new CustomerLookupResponse(user.getPublicId(), user.getFullName());
    }
}
