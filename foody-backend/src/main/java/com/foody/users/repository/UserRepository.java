package com.foody.users.repository;

import com.foody.users.entity.User;
import com.foody.users.entity.UserRole;
import com.foody.users.entity.UserStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByPublicIdAndRole(String publicId, UserRole role);

    Optional<User> findByPublicId(String publicId);

    boolean existsByPublicId(String publicId);

    // Admin panel: full user list, optionally filtered by role and/or status.
    // Passing null for a param means "don't filter on it" — mirrors BusinessRepository.search.
    @Query("""
            SELECT u FROM User u
            WHERE (:role IS NULL OR u.role = :role)
              AND (:status IS NULL OR u.status = :status)
            ORDER BY u.createdAt DESC
            """)
    List<User> search(@Param("role") UserRole role, @Param("status") UserStatus status);
}
