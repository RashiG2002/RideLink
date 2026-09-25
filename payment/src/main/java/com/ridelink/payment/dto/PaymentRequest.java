package com.ridelink.payment.dto;

import com.ridelink.payment.model.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Holds the validated details needed to create or process a ride payment. */
public record PaymentRequest(
    // 1. Identify the ride the payment belongs to.
    @NotBlank(message = "Ride ID is required")
    String rideId,
    
    // 2. Identify the passenger making the payment.
    @NotBlank(message = "Passenger ID is required")
    String passengerId,
    
    // 3. Require a non-null amount greater than zero.
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be strictly positive")
    Double amount,
    
    // 4. Select a payment method; FAIL and DECLINED are demo options for deterministic failures.
    @NotNull(message = "Payment method is required")
    @Schema(description = "Selected payment method. FAIL and DECLINED are deterministic demo failure options.",
            allowableValues = {"CARD", "CASH", "WALLET", "FAIL", "DECLINED"})
    PaymentMethod paymentMethod
) {}