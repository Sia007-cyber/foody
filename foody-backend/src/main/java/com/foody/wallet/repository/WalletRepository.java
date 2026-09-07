package com.foody.wallet.repository;
import com.foody.wallet.entity.Wallet; import jakarta.persistence.LockModeType; import java.util.*;
import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface WalletRepository extends JpaRepository<Wallet,Long>{
 Optional<Wallet> findByCustomerUserIdAndBusinessId(Long customerUserId,Long businessId);
 List<Wallet> findByCustomerUserIdOrderByCreatedAtDesc(Long customerUserId);
 List<Wallet> findByBusinessIdOrderByCreatedAtDesc(Long businessId);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select w from Wallet w where w.id=:id") Optional<Wallet> findByIdForUpdate(@Param("id") Long id);
}
