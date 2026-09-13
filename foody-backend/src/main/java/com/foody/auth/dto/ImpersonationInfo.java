package com.foody.auth.dto;

public record ImpersonationInfo(
        boolean active,
        Long initiatingAdminId,
        String sessionId) { }
