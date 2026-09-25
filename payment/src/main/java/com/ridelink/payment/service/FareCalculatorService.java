package com.ridelink.payment.service;

import org.springframework.stereotype.Service;

/**
 * @author Paboda Diwyanjalee (IT24104072)
 */
@Service
public class FareCalculatorService {
    // Pricing components used to calculate the estimated fare.
    private static final double BASE_FARE = 250.00;
    private static final double PER_KM_RATE = 100.00;
    private static final double SERVICE_CHARGE = 50.00;

    public Double estimateFare(Double distanceInKm) {
        // 1. Reject missing, non-finite, or non-positive distances.
        validateDistance(distanceInKm);
        // 2. Add the base fare, distance charge, and fixed service charge.
        return BASE_FARE + (distanceInKm * PER_KM_RATE) + SERVICE_CHARGE;
    }

    public Double calculateFinalFare(Double actualDistanceInKm, boolean isPeakHour) {
        // 1. Validate the actual trip distance before calculating its fare.
        validateDistance(actualDistanceInKm);
        // 2. Calculate the standard fare, then apply a 50% peak-hour increase if needed.
        double fare = estimateFare(actualDistanceInKm);
        return isPeakHour ? fare * 1.5 : fare;
    }

    private void validateDistance(Double distanceInKm) {
        // Reject values that cannot represent a valid positive distance.
        if (distanceInKm == null || !Double.isFinite(distanceInKm) || distanceInKm <= 0) {
            throw new IllegalArgumentException("Distance must be a finite positive number");
        }
    }
}