package com.ridelink.payment.model;

public enum PaymentMethod {
    // 1. Normal payment methods accepted by the payment flow.
    CARD,
    CASH,
    WALLET,

    // 2. Demo options that force a failed payment outcome.
    FAIL,
    DECLINED
}