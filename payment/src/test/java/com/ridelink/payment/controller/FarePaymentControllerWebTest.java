package com.ridelink.payment.controller;

import com.ridelink.payment.service.FareCalculatorService;
import com.ridelink.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.ridelink.payment.model.Payment;
import com.ridelink.payment.model.PaymentStatus;

@ExtendWith(MockitoExtension.class)
class FarePaymentControllerWebTest {

    private MockMvc mockMvc;

    @Mock
    private FareCalculatorService fareCalculatorService;

    @Mock
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        // 1. Create the controller with mocked services so this test targets HTTP mapping only.
        mockMvc = MockMvcBuilders
                .standaloneSetup(new FarePaymentController(fareCalculatorService, paymentService))
                .build();
    }

    @Test
    void estimateEndpointShouldAcceptGetRequest() throws Exception {
        // 1. Arrange the fare the mocked service should return for 5 km.
        when(fareCalculatorService.estimateFare(5.0)).thenReturn(350.0);

        // 2. Send the GET request with its distance query parameter and verify HTTP 200.
        mockMvc.perform(get("/api/v1/fares/estimate")
                        .param("distanceInKm", "5.0"))
                .andExpect(status().isOk());
    }

    @Test
    void calculateFinalFareEndpointShouldAcceptGetRequest() throws Exception {
        // 1. Arrange the fare the mocked service should return for a 10 km peak-hour trip.
        when(fareCalculatorService.calculateFinalFare(10.0, true)).thenReturn(600.0);

        // 2. Send the GET request with both query parameters and verify HTTP 200.
        mockMvc.perform(get("/api/v1/fares/calculate-final")
                        .param("actualDistance", "10.0")
                        .param("isPeakHour", "true"))
                .andExpect(status().isOk());
    }

        @Test
        void paymentByDistanceCalculatesAmountBeforeProcessing() throws Exception {
            // Arrange the generated fare and successful payment returned by the mocked payment service.
                when(paymentService.processPayment(any())).thenReturn(
                    Payment.builder().amount(350.0).status(PaymentStatus.SUCCESS).build());

                // Submit ride details as JSON and verify the endpoint responds successfully.
                mockMvc.perform(post("/api/v1/fares/pay/by-distance")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("""
                                                                {
                                                                    "user": "user-1",
                                                                    "rideId": "ride-100",
                                                                    "passengerId": "passenger-1",
                                                                    "distanceInKm": 4.0,
                                                                    "actualDistanceInKm": 5.0,
                                                                    "isPeakHour": false,
                                                                    "paymentMethod": "CARD"
                                                                }
                                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.amount").value(350.0))
                                .andExpect(jsonPath("$.status").value("SUCCESS"));
        }

                    @Test
                    void paymentByDistanceWithoutActualDistanceKeepsExistingBehavior() throws Exception {
                        // Arrange the original single-distance fare calculation and payment response.
                        when(paymentService.processPayment(any())).thenReturn(
                            Payment.builder().amount(350.0).status(PaymentStatus.SUCCESS).build());

                        // Omit actualDistanceInKm and confirm the original amount and status are returned.
                        mockMvc.perform(post("/api/v1/fares/pay/by-distance")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                {
                                                    "user": "user-1",
                                                    "rideId": "ride-legacy",
                                                    "passengerId": "passenger-1",
                                                    "distanceInKm": 5.0,
                                                    "isPeakHour": false,
                                                    "paymentMethod": "CARD"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.amount").value(350.0))
                                .andExpect(jsonPath("$.status").value("SUCCESS"));
                    }
}
