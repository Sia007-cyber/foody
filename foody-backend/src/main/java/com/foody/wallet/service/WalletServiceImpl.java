package com.foody.wallet.service;

import com.foody.common.exception.InsufficientBalanceException;
import com.foody.wallet.dto.WalletResponse;
import com.foody.wallet.dto.WalletTransactionResponse;
import com.foody.wallet.entity.Wallet;
import com.foody.wallet.entity.WalletTransaction;
import com.foody.wallet.entity.WalletTransactionType;
import com.foody.wallet.repository.WalletRepository;
import com.foody.wallet.repository.WalletTransactionRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    WalletServiceImpl(WalletRepository walletRepository, WalletTransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional
    public WalletResponse getBalance(Long userId) {
        return WalletResponse.from(getOrCreateWallet(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletTransactionResponse> getTransactions(Long userId) {
        return walletRepository.findByUserId(userId)
                .map(wallet -> transactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId()).stream()
                        .map(WalletTransactionResponse::from)
                        .toList())
                // No wallet yet means no transactions yet — an empty list, not a 404;
                // a wallet that's never been touched isn't an error state for this read.
                .orElseGet(List::of);
    }

    @Override
    @Transactional
    public WalletResponse topUp(Long userId, BigDecimal amount) {
        WalletTransactionResponse tx = applyCredit(getOrCreateWallet(userId), amount, WalletTransactionType.TOPUP,
                null, null, "شارژ کیف پول");
        return new WalletResponse(tx.balanceAfter());
    }

    @Override
    @Transactional
    public WalletTransactionResponse debit(Long userId, BigDecimal amount, String referenceType,
                                           Long referenceId, String description) {
        Wallet wallet = getOrCreateWallet(userId);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    "Wallet balance " + wallet.getBalance() + " is insufficient for a debit of " + amount);
        }
        wallet.setBalance(wallet.getBalance().subtract(amount));
        Wallet saved = walletRepository.save(wallet);

        WalletTransaction tx = new WalletTransaction();
        tx.setWalletId(saved.getId());
        tx.setType(WalletTransactionType.ORDER_PAYMENT);
        tx.setCredit(false);
        tx.setAmount(amount);
        tx.setBalanceAfter(saved.getBalance());
        tx.setDescription(description);
        tx.setReferenceType(referenceType);
        tx.setReferenceId(referenceId);
        return WalletTransactionResponse.from(transactionRepository.save(tx));
    }

    @Override
    @Transactional
    public WalletTransactionResponse credit(Long userId, BigDecimal amount, WalletTransactionType type,
                                            String referenceType, Long referenceId, String description) {
        return applyCredit(getOrCreateWallet(userId), amount, type, referenceType, referenceId, description);
    }

    private WalletTransactionResponse applyCredit(Wallet wallet, BigDecimal amount, WalletTransactionType type,
                                                   String referenceType, Long referenceId, String description) {
        wallet.setBalance(wallet.getBalance().add(amount));
        Wallet saved = walletRepository.save(wallet);

        WalletTransaction tx = new WalletTransaction();
        tx.setWalletId(saved.getId());
        tx.setType(type);
        tx.setCredit(true);
        tx.setAmount(amount);
        tx.setBalanceAfter(saved.getBalance());
        tx.setDescription(description);
        tx.setReferenceType(referenceType);
        tx.setReferenceId(referenceId);
        return WalletTransactionResponse.from(transactionRepository.save(tx));
    }

    private Wallet getOrCreateWallet(Long userId) {
        return walletRepository.findByUserId(userId).orElseGet(() -> {
            Wallet wallet = new Wallet();
            wallet.setUserId(userId);
            wallet.setBalance(BigDecimal.ZERO);
            return walletRepository.save(wallet);
        });
    }
}
