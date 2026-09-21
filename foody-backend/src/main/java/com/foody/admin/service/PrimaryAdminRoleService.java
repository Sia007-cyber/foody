package com.foody.admin.service;

import com.foody.admin.entity.AdminRoleAction;
import com.foody.admin.entity.AdminRoleAudit;
import com.foody.admin.repository.AdminRoleAuditRepository;
import com.foody.auth.security.FoodyUserPrincipal;
import com.foody.auth.service.RoleChangeSessionService;
import com.foody.common.exception.InvalidStateTransitionException;
import com.foody.common.exception.ResourceNotFoundException;
import com.foody.users.entity.User;
import com.foody.users.entity.UserRole;
import com.foody.users.entity.UserStatus;
import com.foody.users.service.UserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The only application service allowed to grant or revoke the ADMIN role. */
@Service
public class PrimaryAdminRoleService {
    private final UserService users;
    private final AdminRoleAuditRepository audits;
    private final RoleChangeSessionService sessions;

    public PrimaryAdminRoleService(UserService users, AdminRoleAuditRepository audits,
                                   RoleChangeSessionService sessions) {
        this.users = users;
        this.audits = audits;
        this.sessions = sessions;
    }

    @Transactional
    public User grantAdmin(FoodyUserPrincipal principal, Long targetId) {
        User actor = requirePrimaryAdmin(principal);
        User target = lockTarget(actor, targetId);
        if (target.isPrimaryAdmin() || target.getRole() == UserRole.ADMIN) {
            throw new InvalidStateTransitionException("The target already has administrator privileges");
        }
        if (target.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidStateTransitionException("Only active users can be granted administrator privileges");
        }
        UserRole previousRole = target.getRole();
        if (previousRole != UserRole.CUSTOMER && previousRole != UserRole.BUSINESS_OWNER) {
            throw new InvalidStateTransitionException("The target role is not eligible for administrator privileges");
        }

        target.setAdminBaseRole(previousRole);
        target.setRole(UserRole.ADMIN);
        User saved = users.save(target);
        audits.save(new AdminRoleAudit(actor, saved, previousRole, UserRole.ADMIN, AdminRoleAction.GRANT_ADMIN));
        sessions.invalidate(saved.getId());
        return saved;
    }

    @Transactional
    public User revokeAdmin(FoodyUserPrincipal principal, Long targetId) {
        User actor = requirePrimaryAdmin(principal);
        User target = lockTarget(actor, targetId);
        if (target.isPrimaryAdmin()) {
            throw new InvalidStateTransitionException("The primary admin cannot be demoted");
        }
        if (target.getRole() != UserRole.ADMIN) {
            throw new InvalidStateTransitionException("The target is not an administrator");
        }

        UserRole restoredRole = target.getAdminBaseRole() == null
                ? UserRole.CUSTOMER
                : target.getAdminBaseRole();
        target.setRole(restoredRole);
        target.setAdminBaseRole(null);
        User saved = users.save(target);
        audits.save(new AdminRoleAudit(actor, saved, UserRole.ADMIN, restoredRole, AdminRoleAction.REVOKE_ADMIN));
        sessions.invalidate(saved.getId());
        return saved;
    }

    private User requirePrimaryAdmin(FoodyUserPrincipal principal) {
        if (principal == null || principal.isImpersonating()) {
            throw new AccessDeniedException("A non-impersonated primary-admin session is required");
        }
        User actor = users.findByIdForUpdate(principal.getUserId())
                .orElseThrow(() -> new AccessDeniedException("Primary-admin account is unavailable"));
        if (!actor.isPrimaryAdmin() || actor.getRole() != UserRole.ADMIN || actor.getStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedException("Only the primary admin may manage administrator privileges");
        }
        return actor;
    }

    private User lockTarget(User actor, Long targetId) {
        if (actor.getId().equals(targetId)) {
            throw new InvalidStateTransitionException("The primary admin cannot change its own administrator role");
        }
        return users.findByIdForUpdate(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
