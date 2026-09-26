package com.ridelink.payment.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FareCalculatorServiceTest {

    private final FareCalculatorService service = new FareCalculatorService();

    @Test
    void calculatesStandardFare() {
        // Verify that base fare, distance charge, and service charge total 350 for 5 km.
        assertEquals(350.0, service.estimateFare(5.0));
    }

    @Test
    void appliesPeakMultiplierToFinalFare() {
        // Verify that peak-hour pricing raises the standard 5 km fare by 50 percent.
        assertEquals(525.0, service.calculateFinalFare(5.0, true));
    }

    @Test
    void adjustsFareByTheDifferenceBetweenEnteredAndActualDistance() {
        // Both entered estimates are corrected to the fare for the measured 5 km distance.
        assertEquals(350.0, service.calculateAdjustedFare(4.0, 5.0, false));

        assertEquals(350.0, service.calculateAdjustedFare(6.0, 5.0, false));
        assertEquals(service.calculateFinalFare(5.0, false), service.calculateAdjustedFare(6.0, 5.0, false));
    }

    @Test
    void appliesPeakMultiplierToAdjustedFareAndRejectsInvalidResult() {
        // Apply peak pricing to the fare calculated from the measured distance.
        assertEquals(525.0, service.calculateAdjustedFare(4.0, 5.0, true));

        // Reject a measured distance whose calculated fare overflows to a non-finite value.
        assertThrows(IllegalArgumentException.class,
                () -> service.calculateAdjustedFare(1.0, Double.MAX_VALUE, false));
    }

    @Test
    void rejectsNonPositiveDistance() {
        // Verify that zero and negative distances are rejected by both fare calculations.
        assertThrows(IllegalArgumentException.class, () -> service.estimateFare(0.0));
        assertThrows(IllegalArgumentException.class, () -> service.calculateFinalFare(-1.0, false));
    }
}
