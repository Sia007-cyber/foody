package com.foody.auth.repository;

import com.foody.auth.entity.ImpersonationSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImpersonationSessionRepository extends JpaRepository<ImpersonationSession, String> {
    @Modifying
    @Query("update ImpersonationSession s set s.endedAt = CURRENT_TIMESTAMP " +
            "where s.admin.id = :adminId and s.endedAt is null")
    int endAllActiveByAdminId(@Param("adminId") Long adminId);
}
