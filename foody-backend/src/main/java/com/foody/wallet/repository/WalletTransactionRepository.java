package com.foody.wallet.repository;
import com.foody.wallet.entity.WalletTransaction; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction,Long>{List<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(Long walletId);}
