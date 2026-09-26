package com.ridelink.payment.service;

import org.springframework.stereotype.Service;

/**
 * @author Paboda Diwyanjalee (IT24104072)
 */
@Service
public class FareCalculatorService {
    // Pricing components used to calculate the estimated fare.
    private static final double BASE_FARE = 250.00;
    private static final double PER_KM_RATE = 10.00;
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

    public Double calculateAdjustedFare(
            Double enteredDistanceInKm, Double actualDistanceInKm, boolean isPeakHour) {
        // Validate both values before comparing the passenger estimate with the measured distance.
        validateDistance(enteredDistanceInKm);
        validateDistance(actualDistanceInKm);

        // Use measured distance for final billing; this is equivalent to correcting the estimate by the distance difference.
        double finalFare = calculateFinalFare(actualDistanceInKm, isPeakHour);

        // Reject overflow or any invalid non-positive final fare.
        if (!Double.isFinite(finalFare) || finalFare <= 0) {
            throw new IllegalArgumentException("Adjusted fare must be a finite positive number");
        }
        return finalFare;
    }

    private void validateDistance(Double distanceInKm) {
        // Reject values that cannot represent a valid positive distance.
        if (distanceInKm == null || !Double.isFinite(distanceInKm) || distanceInKm <= 0) {
            throw new IllegalArgumentException("Distance must be a finite positive number");
        }
    }
}