package com.foody.wallet.entity;

/**
 * What kind of event moved money in/out of a wallet. TOPUP is currently simulated —
 * there is no real payment gateway wired in yet (locked scope decision, see root
 * README); it directly credits the wallet the moment the user requests it.
 */
public enum WalletTransactionType {
    TOPUP,           // credit — user-initiated top-up (simulated, no gateway yet)
    ORDER_PAYMENT,   // debit — paid for an order using wallet balance
    REFUND,          // credit — order cancelled/rejected after wallet payment
    CREDIT_REWARD,   // credit — referral bonus, mission reward, promo credit
    ADMIN_ADJUSTMENT // credit or debit — manual correction by an admin
}
