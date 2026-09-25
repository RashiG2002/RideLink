package com.ridelink.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Carries the distance input used by the fare-estimate endpoint. */
public record FareEstimateRequest(
    // Reject a missing distance and any value that is zero or below.
    @NotNull(message = "Distance cannot be null")
    @Positive(message = "Distance must be positive")
    Double distanceInKm
) {}