package com.ridelink.payment.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FareCalculatorServiceTest {

    private final FareCalculatorService service = new FareCalculatorService();

    @Test
    void calculatesStandardFare() {
        assertEquals(800.0, service.estimateFare(5.0));
    }

    @Test
    void appliesPeakMultiplierToFinalFare() {
        assertEquals(1_200.0, service.calculateFinalFare(5.0, true));
    }

    @Test
    void rejectsNonPositiveDistance() {
        assertThrows(IllegalArgumentException.class, () -> service.estimateFare(0.0));
        assertThrows(IllegalArgumentException.class, () -> service.calculateFinalFare(-1.0, false));
    }
}
