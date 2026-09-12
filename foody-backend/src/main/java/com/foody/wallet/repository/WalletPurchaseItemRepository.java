package com.foody.wallet.repository;
import com.foody.wallet.entity.WalletPurchaseItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface WalletPurchaseItemRepository extends JpaRepository<WalletPurchaseItem,Long> {
    List<WalletPurchaseItem> findByDebitRequestIdOrderById(Long requestId);
}
