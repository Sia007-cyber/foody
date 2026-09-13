package com.foody.auth.service;

import com.foody.auth.dto.TokenResponse;
import com.foody.auth.entity.ImpersonationSession;
import com.foody.auth.entity.RefreshTokenSession;
import com.foody.auth.repository.ImpersonationSessionRepository;
import com.foody.auth.repository.RefreshTokenSessionRepository;
import com.foody.auth.security.FoodyUserPrincipal;
import com.foody.auth.security.JwtService;
import com.foody.common.exception.InvalidRequestException;
import com.foody.common.exception.ResourceNotFoundException;
import com.foody.users.entity.User;
import com.foody.users.entity.UserRole;
import com.foody.users.entity.UserStatus;
import com.foody.users.service.UserService;
import io.jsonwebtoken.Claims;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAccountSupportService {
    private static final long IMPERSONATION_TTL_SECONDS = Duration.ofMinutes(30).toSeconds();
    private final UserService users;
    private final JwtService jwt;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenSessionRepository refreshSessions;
    private final ImpersonationSessionRepository impersonations;

    public AdminAccountSupportService(UserService users, JwtService jwt, PasswordEncoder passwordEncoder,
            RefreshTokenSessionRepository refreshSessions, ImpersonationSessionRepository impersonations) {
        this.users = users;
        this.jwt = jwt;
        this.passwordEncoder = passwordEncoder;
        this.refreshSessions = refreshSessions;
        this.impersonations = impersonations;
    }

    @Transactional
    public TokenResponse startImpersonation(FoodyUserPrincipal actor, Long targetId) {
        requireRealAdmin(actor);
        User target = users.findById(targetId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (target.getRole() == UserRole.ADMIN) throw new AccessDeniedException("Admin accounts cannot be impersonated");
        if (target.getStatus() != UserStatus.ACTIVE) throw new InvalidRequestException("Only active accounts can be impersonated");
        String sessionId = UUID.randomUUID().toString();
        ImpersonationSession audit = new ImpersonationSession();
        audit.setId(sessionId);
        audit.setAdmin(actor.getUser());
        audit.setTarget(target);
        impersonations.save(audit);
        return issue(target, actor.getUserId(), audit);
    }

    @Transactional
    public void resetPassword(FoodyUserPrincipal actor, Long targetId, String newPassword) {
        requireRealAdmin(actor);
        User target = users.findById(targetId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (target.getRole() == UserRole.ADMIN) throw new AccessDeniedException("Admin passwords cannot be reset here");
        target.setPasswordHash(passwordEncoder.encode(newPassword));
        users.save(target);
        refreshSessions.revokeAllActiveByUserId(targetId);
    }

    @Transactional
    public void exitImpersonation(FoodyUserPrincipal actor) {
        if (!actor.isImpersonating()) throw new AccessDeniedException("No impersonation session is active");
        ImpersonationSession audit = impersonations.findById(actor.getImpersonationSessionId())
                .orElseThrow(() -> new AccessDeniedException("Unknown impersonation session"));
        if (!audit.getTarget().getId().equals(actor.getUserId()) ||
                !audit.getAdmin().getId().equals(actor.getInitiatingAdminId())) {
            throw new AccessDeniedException("Invalid impersonation context");
        }
        if (audit.getEndedAt() == null) audit.setEndedAt(Instant.now());
        refreshSessions.revokeAllActiveByImpersonationSessionId(audit.getId());
        impersonations.save(audit);
    }

    @Transactional
    public TokenResponse rotate(RefreshTokenSession current, Claims claims) {
        ImpersonationSession audit = current.getImpersonationSession();
        if (audit == null || audit.getEndedAt() != null ||
                !audit.getId().equals(jwt.getImpersonationSessionId(claims)) ||
                !audit.getAdmin().getId().equals(jwt.getInitiatingAdminId(claims))) {
            throw new com.foody.common.exception.InvalidCredentialsException("Invalid impersonation session");
        }
        return issue(current.getUser(), audit.getAdmin().getId(), audit);
    }

    private TokenResponse issue(User target, Long adminId, ImpersonationSession audit) {
        String access = jwt.generateImpersonationAccessToken(target, adminId, audit.getId());
        String refresh = jwt.generateImpersonationRefreshToken(target, adminId, audit.getId());
        RefreshTokenSession session = new RefreshTokenSession();
        session.setUser(target);
        session.setImpersonationSession(audit);
        session.setTokenHash(hash(refresh));
        session.setExpiresAt(jwt.parse(refresh).getExpiration().toInstant());
        refreshSessions.save(session);
        return new TokenResponse(access, refresh, "Bearer", IMPERSONATION_TTL_SECONDS,
                new com.foody.auth.dto.ImpersonationInfo(true, adminId, audit.getId()));
    }

    private static void requireRealAdmin(FoodyUserPrincipal actor) {
        if (actor == null || actor.getUser().getRole() != UserRole.ADMIN || actor.isImpersonating()) {
            throw new AccessDeniedException("A non-impersonated admin session is required");
        }
    }

    private static String hash(String token) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
