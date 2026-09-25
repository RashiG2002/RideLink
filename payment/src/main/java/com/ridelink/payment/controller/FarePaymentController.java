package com.ridelink.payment.controller;

import com.ridelink.payment.dto.FareEstimateRequest;
import com.ridelink.payment.dto.PaymentRequest;
import com.ridelink.payment.model.Payment;
import com.ridelink.payment.model.PaymentMethod;
import com.ridelink.payment.model.Receipt;
import com.ridelink.payment.service.FareCalculatorService;
import com.ridelink.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fares")
@RequiredArgsConstructor
@Tag(name = "Fare & Payment API", description = "Operations for fare estimation, payment simulation, and receipt retrieval")
public class FarePaymentController {

    // Spring injects the services that contain the fare and payment business logic.
    private final FareCalculatorService fareCalculatorService;
    private final PaymentService paymentService;

    // 1. Read the distance from the query string, then return the estimated fare.
    @GetMapping("/estimate")
    @Operation(summary = "Get Fare Estimate", description = "Calculates estimated fare based on distance")
    @ApiResponse(responseCode = "200", description = "Successful fare estimation")
    public ResponseEntity<Double> getEstimate(@RequestParam Double distanceInKm) {
        Double estimate = fareCalculatorService.estimateFare(distanceInKm);
        return ResponseEntity.ok(estimate);
    }

    // 1. Read and validate the distance from a JSON body, then return the same estimate.
    @PostMapping("/estimate")
    @Operation(summary = "Get Fare Estimate", description = "Calculates estimated fare based on distance")
    @ApiResponse(responseCode = "200", description = "Successful fare estimation")
    public ResponseEntity<Double> getEstimate(@Valid @RequestBody FareEstimateRequest request) {
        Double estimate = fareCalculatorService.estimateFare(request.distanceInKm());
        return ResponseEntity.ok(estimate);
    }

    // 1. Accept trip distance and peak-hour status, then return the surge-adjusted fare.
    @GetMapping("/calculate-final")
    @Operation(summary = "Calculate Final Fare", description = "Determines final fare factoring in surge pricing")
    public ResponseEntity<Double> calculateFinalFare(
            @RequestParam Double actualDistance,
            @RequestParam boolean isPeakHour) {
        return ResponseEntity.ok(fareCalculatorService.calculateFinalFare(actualDistance, isPeakHour));
    }

    // 1. POST variant of the final-fare endpoint; inputs are still supplied as query parameters.
    @PostMapping("/calculate-final")
    @Operation(summary = "Calculate Final Fare", description = "Determines final fare factoring in surge pricing")
    public ResponseEntity<Double> calculateFinalFarePost(
            @RequestParam Double actualDistance,
            @RequestParam boolean isPeakHour) {
        return ResponseEntity.ok(fareCalculatorService.calculateFinalFare(actualDistance, isPeakHour));
    }

    // 1. Convert query parameters into the shared payment request format.
    // 2. Delegate to the common payment flow so GET and POST behave consistently.
    @GetMapping("/pay")
    @Operation(summary = "Process Simulated Payment", description = "Simulates a payment transaction")
    public ResponseEntity<Payment> processPayment(
            @RequestParam String rideId,
            @RequestParam String passengerId,
            @RequestParam Double amount,
            @RequestParam PaymentMethod paymentMethod) {
        PaymentRequest request = new PaymentRequest(rideId, passengerId, amount, paymentMethod);
        return processPaymentInternal(request);
    }

    // 1. Validate the JSON payment request, then delegate to the shared payment flow.
    @PostMapping("/pay")
    @Operation(summary = "Process Simulated Payment", description = "Simulates a payment transaction")
    public ResponseEntity<Payment> processPayment(@Valid @RequestBody PaymentRequest request) {
        return processPaymentInternal(request);
    }

    // 1. Validate the payment details, then create a pending record for later processing.
    @PostMapping("/pay/pending")
    @Operation(summary = "Create Pending Payment", description = "Creates a pending payment for asynchronous processing demonstration")
    @ApiResponse(responseCode = "201", description = "Pending payment created")
    public ResponseEntity<Payment> createPendingPayment(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.createPendingPayment(request));
    }

    private ResponseEntity<Payment> processPaymentInternal(PaymentRequest request) {
        // 1. Ask the service to process the transaction.
        Payment payment = paymentService.processPayment(request);
        // 2. Represent a failed transaction as HTTP 400; successful payments return HTTP 200.
        if (payment.getStatus().name().equals("FAILED")) {
            return ResponseEntity.badRequest().body(payment);
        }
        return ResponseEntity.ok(payment);
    }

    // 1. Look up the receipt using the ride ID and return it, or let the exception handler report not found.
    @GetMapping("/receipt/{rideId}")
    @Operation(summary = "Retrieve Receipt", description = "Fetches a generated receipt by Ride ID")
    @ApiResponse(responseCode = "200", description = "Receipt found")
    @ApiResponse(responseCode = "404", description = "Receipt not found")
    public ResponseEntity<Receipt> getReceipt(@PathVariable String rideId) {
        return ResponseEntity.ok(paymentService.getReceiptByRideId(rideId));
    }

    // 1. Look up the payment and its status using the ride ID.
    @GetMapping("/payment/{rideId}")
    @Operation(summary = "Retrieve Payment", description = "Fetches the payment record and status by ride ID")
    @ApiResponse(responseCode = "200", description = "Payment found")
    @ApiResponse(responseCode = "404", description = "Payment not found")
    public ResponseEntity<Payment> getPayment(@PathVariable String rideId) {
        return ResponseEntity.ok(paymentService.getPaymentByRideId(rideId));
    }

    // 1. Ask the service to process the pending payment identified by the ride ID.
    @PostMapping("/payment/{rideId}/process")
    @Operation(summary = "Process Pending Payment", description = "Updates a pending payment to SUCCESS or FAILED")
    @ApiResponse(responseCode = "200", description = "Payment processed")
    @ApiResponse(responseCode = "404", description = "Payment not found")
    @ApiResponse(responseCode = "409", description = "Payment is not pending")
    public ResponseEntity<Payment> processPendingPayment(@PathVariable String rideId) {
        return ResponseEntity.ok(paymentService.processPendingPayment(rideId));
    }
}