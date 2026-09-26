package com.ridelink.payment.model;

public enum PaymentStatus {
    // 1. Payment has been recorded but still needs processing.
    PENDING,
    // 2. Processing completed successfully.
    SUCCESS,
    // 3. Processing completed with a failed outcome.
    FAILED
}