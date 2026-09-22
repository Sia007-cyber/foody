package com.foody.wallet.dto;

import java.util.List;

/** Bounded owner-facing search page; customer records remain deliberately minimal. */
public record CustomerSearchResponse(List<CustomerLookupResponse> items, int page, boolean hasMore) {}
