package com.foody.wallet.repository;
import com.foody.wallet.entity.*; import jakarta.persistence.LockModeType; import java.util.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface OwnerDebitRequestRepository extends JpaRepository<OwnerDebitRequest,Long>{
 List<OwnerDebitRequest> findByCustomerUserIdAndStatusOrderByCreatedAtDesc(Long id,DebitRequestStatus status);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from OwnerDebitRequest r where r.id=:id") Optional<OwnerDebitRequest> findByIdForUpdate(@Param("id") Long id);
}
