package com.foody.admin.repository;

import com.foody.admin.entity.AdminRoleAction;
import com.foody.admin.entity.AdminRoleAudit;
import java.util.Optional;
import org.springframework.data.repository.Repository;

/** Append/query-only repository: deletion and mutation operations are deliberately absent. */
public interface AdminRoleAuditRepository extends Repository<AdminRoleAudit, Long> {
    AdminRoleAudit save(AdminRoleAudit audit);
    long countByTargetIdAndActionType(Long targetId, AdminRoleAction actionType);
    Optional<AdminRoleAudit> findTopByTargetIdOrderByIdDesc(Long targetId);
}
