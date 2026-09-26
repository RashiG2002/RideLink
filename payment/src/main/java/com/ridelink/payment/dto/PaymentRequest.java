package com.ridelink.payment.dto;

import com.ridelink.payment.model.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Holds the validated details needed to create or process a ride payment. */
public record PaymentRequest(
    // Identify the account user and ride passenger associated with this payment.
    @NotBlank(message = "User is required")
    @Schema(type = "string", example = "user-1", description = "User identity forwarded by the trusted caller")
    String user,

    @NotBlank(message = "Ride ID is required")
    @Schema(type = "string", example = "ride-100", description = "Stable ride identifier")
    String rideId,

    @NotBlank(message = "Passenger ID is required")
    @Schema(type = "string", example = "passenger-1", description = "Stable passenger identifier")
    String passengerId,

    @NotNull(message = "Distance is required")
    @Positive(message = "Distance must be strictly positive")
    @Schema(type = "number", format = "double", example = "5.0", minimum = "0.01",
        description = "Entered ride distance in kilometres")
    Double distanceInKm,

    @Positive(message = "Actual distance must be strictly positive")
    @Schema(type = "number", format = "double", example = "6.0", minimum = "0.01",
        description = "Optional measured ride distance used to calculate the final fare")
    Double actualDistanceInKm,

    @Schema(type = "boolean", example = "false", description = "Whether peak-hour pricing applies")
    boolean isPeakHour,

    // Select a payment method; FAIL and DECLINED are demo options for deterministic failures.
    @NotNull(message = "Payment method is required")
    @Schema(description = "Selected payment method. FAIL and DECLINED are deterministic demo failure options.",
            allowableValues = {"CARD", "CASH", "WALLET", "FAIL", "DECLINED"})
    PaymentMethod paymentMethod
) {}