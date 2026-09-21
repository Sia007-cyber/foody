package com.foody.auth.service;

import com.foody.auth.repository.ImpersonationSessionRepository;
import com.foody.auth.repository.RefreshTokenSessionRepository;
import org.springframework.stereotype.Service;

/** Invalidates every renewable or delegated session affected by an account role change. */
@Service
public class RoleChangeSessionService {
    private final RefreshTokenSessionRepository refreshSessions;
    private final ImpersonationSessionRepository impersonationSessions;

    public RoleChangeSessionService(RefreshTokenSessionRepository refreshSessions,
                                    ImpersonationSessionRepository impersonationSessions) {
        this.refreshSessions = refreshSessions;
        this.impersonationSessions = impersonationSessions;
    }

    public void invalidate(Long userId) {
        refreshSessions.revokeAllActiveByUserId(userId);
        refreshSessions.revokeAllActiveImpersonationSessionsByAdminId(userId);
        impersonationSessions.endAllActiveByAdminId(userId);
    }
}
