package com.ridelink.payment.controller;

import com.ridelink.payment.dto.FareEstimateRequest;
import com.ridelink.payment.dto.PaymentRequest;
import com.ridelink.payment.model.Payment;
import com.ridelink.payment.model.PaymentMethod;
import com.ridelink.payment.model.Receipt;
import com.ridelink.payment.service.FareCalculatorService;
import com.ridelink.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
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
        @Operation(summary = "1. Estimate Fare",
            description = "Enter distanceInKm first. Calculates 250 + (distanceInKm * 10) + 50.")
    @ApiResponse(responseCode = "200", description = "Successful fare estimation")
        public ResponseEntity<Double> getEstimate(
            @Parameter(description = "Trip distance in kilometres", example = "5.0",
                schema = @Schema(type = "number", format = "double", minimum = "0.01"))
            @RequestParam Double distanceInKm) {
        Double estimate = fareCalculatorService.estimateFare(distanceInKm);
        return ResponseEntity.ok(estimate);
    }

    // 1. Read and validate the distance from a JSON body, then return the same estimate.
    @PostMapping("/estimate")
        @Operation(summary = "1. Estimate Fare",
            description = "Enter distanceInKm first. Calculates 250 + (distanceInKm * 10) + 50.")
    @ApiResponse(responseCode = "200", description = "Successful fare estimation")
    public ResponseEntity<Double> getEstimate(@Valid @RequestBody FareEstimateRequest request) {
        Double estimate = fareCalculatorService.estimateFare(request.distanceInKm());
        return ResponseEntity.ok(estimate);
    }

    // 1. Accept trip distance and peak-hour status, then return the surge-adjusted fare.
    @GetMapping("/calculate-final")
        @Operation(summary = "2. Calculate Final Fare",
            description = "Enter actualDistance and isPeakHour. Peak-hour pricing multiplies the estimated fare by 1.5.")
    public ResponseEntity<Double> calculateFinalFare(
            @Parameter(description = "Actual trip distance in kilometres", example = "5.0",
                schema = @Schema(type = "number", format = "double", minimum = "0.01"))
            @RequestParam Double actualDistance,
            @Parameter(description = "Whether peak-hour pricing applies", example = "false",
                schema = @Schema(type = "boolean", allowableValues = {"true", "false"}))
            @RequestParam boolean isPeakHour) {
        return ResponseEntity.ok(fareCalculatorService.calculateFinalFare(actualDistance, isPeakHour));
    }

    // 1. POST variant of the final-fare endpoint; inputs are still supplied as query parameters.
    @PostMapping("/calculate-final")
        @Operation(summary = "2. Calculate Final Fare",
            description = "Enter actualDistance and isPeakHour. Peak-hour pricing multiplies the estimated fare by 1.5.")
    public ResponseEntity<Double> calculateFinalFarePost(
            @Parameter(description = "Actual trip distance in kilometres", example = "5.0",
                schema = @Schema(type = "number", format = "double", minimum = "0.01"))
            @RequestParam Double actualDistance,
            @Parameter(description = "Whether peak-hour pricing applies", example = "false",
                schema = @Schema(type = "boolean", allowableValues = {"true", "false"}))
            @RequestParam boolean isPeakHour) {
        return ResponseEntity.ok(fareCalculatorService.calculateFinalFare(actualDistance, isPeakHour));
    }

    // 1. Convert query parameters into the shared payment request format.
    // 2. Delegate to the common payment flow so GET and POST behave consistently.
    @GetMapping("/pay")
        @Operation(summary = "3. Process Simulated Payment",
            description = "Calculates the amount from ride distance and processes the simulated payment.")
    public ResponseEntity<Payment> processPayment(
            @RequestParam String user,
            @RequestParam String rideId,
            @RequestParam String passengerId,
            @RequestParam Double distanceInKm,
            @RequestParam(required = false) Double actualDistanceInKm,
            @RequestParam(defaultValue = "false") boolean isPeakHour,
            @RequestParam PaymentMethod paymentMethod) {
        PaymentRequest request = new PaymentRequest(
                user, rideId, passengerId, distanceInKm, actualDistanceInKm, isPeakHour, paymentMethod);
        return processPaymentInternal(request);
    }

    // 1. Validate the JSON payment request, then delegate to the shared payment flow.
    @PostMapping("/pay")
        @Operation(summary = "3. Process Simulated Payment",
            description = "Calculates the amount from ride distance and processes the simulated payment; do not send an amount.")
    public ResponseEntity<Payment> processPayment(@Valid @RequestBody PaymentRequest request) {
        return processPaymentInternal(request);
    }

        @PostMapping("/pay/by-distance")
            @Operation(summary = "3. Calculate Amount and Process Payment",
            description = "The service calculates the payment amount from distanceInKm and optional actualDistanceInKm; do not send an amount.")
        @ApiResponse(responseCode = "200", description = "Calculated fare paid successfully")
        @ApiResponse(responseCode = "400", description = "Invalid ride details or simulated payment failure")
            @ApiResponse(responseCode = "409", description = "A successful payment already exists for this ride")
        public ResponseEntity<Payment> processPaymentByDistance(
                @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Send user, rideId, passengerId, distanceInKm, optional actualDistanceInKm, isPeakHour, and paymentMethod. The service generates amount.",
                    content = @Content(
                        schema = @Schema(implementation = PaymentRequest.class),
                        examples = @ExampleObject(value = """
                            {
                              "user": "user-1",
                              "rideId": "ride-100",
                              "passengerId": "passenger-1",
                              "distanceInKm": 4.0,
                              "actualDistanceInKm": 5.0,
                              "isPeakHour": false,
                              "paymentMethod": "CARD"
                            }
                            """)))
            @Valid @RequestBody PaymentRequest request) {
        return processPaymentInternal(request);
        }

    // 1. Validate the payment details, then create a pending record for later processing.
    @PostMapping("/pay/pending")
        @Operation(summary = "3. Create Pending Payment",
            description = "Calculates the amount from ride distance and creates a pending payment for later processing.")
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
        @Operation(summary = "5. Retrieve Receipt",
            description = "After a successful payment, retrieve the generated receipt by ride ID.")
    @ApiResponse(responseCode = "200", description = "Receipt found")
    @ApiResponse(responseCode = "404", description = "Receipt not found")
    public ResponseEntity<Receipt> getReceipt(@PathVariable String rideId) {
        return ResponseEntity.ok(paymentService.getReceiptByRideId(rideId));
    }

    // 1. Look up the payment and its status using the ride ID.
    @GetMapping("/payment/{rideId}")
        @Operation(summary = "4. Retrieve Payment",
            description = "Retrieve the saved payment amount and status by ride ID.")
    @ApiResponse(responseCode = "200", description = "Payment found")
    @ApiResponse(responseCode = "404", description = "Payment not found")
    public ResponseEntity<Payment> getPayment(@PathVariable String rideId) {
        return ResponseEntity.ok(paymentService.getPaymentByRideId(rideId));
    }

    // 1. Ask the service to process the pending payment identified by the ride ID.
    @PostMapping("/payment/{rideId}/process")
        @Operation(summary = "3. Process Pending Payment",
            description = "Process the pending payment, then use step 4 to retrieve its status and step 5 to retrieve its receipt.")
    @ApiResponse(responseCode = "200", description = "Payment processed")
    @ApiResponse(responseCode = "404", description = "Payment not found")
    @ApiResponse(responseCode = "409", description = "Payment is not pending")
    public ResponseEntity<Payment> processPendingPayment(@PathVariable String rideId) {
        return ResponseEntity.ok(paymentService.processPendingPayment(rideId));
    }
}