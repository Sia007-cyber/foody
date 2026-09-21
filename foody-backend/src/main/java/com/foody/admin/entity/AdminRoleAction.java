package com.foody.admin.entity;

/** Security-sensitive administrator-role transitions recorded in the audit log. */
public enum AdminRoleAction {
    GRANT_ADMIN,
    REVOKE_ADMIN,
    PROVISION_PRIMARY_ADMIN
}
