package com.foody.admin.entity;

import com.foody.users.entity.User;
import com.foody.users.entity.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Immutable;

/** Append-only audit entry. This entity intentionally exposes no mutators. */
@Entity
@Table(name = "admin_role_audit")
@Immutable
public class AdminRoleAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_primary_admin_user_id", nullable = false, updatable = false)
    private User actor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_user_id", nullable = false, updatable = false)
    private User target;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_role", updatable = false)
    private UserRole previousRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_role", nullable = false, updatable = false)
    private UserRole newRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, updatable = false, length = 40)
    private AdminRoleAction actionType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AdminRoleAudit() { }

    public AdminRoleAudit(User actor, User target, UserRole previousRole, UserRole newRole,
                          AdminRoleAction actionType) {
        this.actor = actor;
        this.target = target;
        this.previousRole = previousRole;
        this.newRole = newRole;
        this.actionType = actionType;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public User getActor() { return actor; }
    public User getTarget() { return target; }
    public UserRole getPreviousRole() { return previousRole; }
    public UserRole getNewRole() { return newRole; }
    public AdminRoleAction getActionType() { return actionType; }
    public Instant getCreatedAt() { return createdAt; }
}
