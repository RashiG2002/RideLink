package com.ridelink.payment.service;

import com.ridelink.payment.dto.PaymentRequest;
import com.ridelink.payment.exception.ResourceNotFoundException;
import com.ridelink.payment.model.Payment;
import com.ridelink.payment.model.PaymentMethod;
import com.ridelink.payment.model.PaymentStatus;
import com.ridelink.payment.model.Receipt;
import com.ridelink.payment.repository.PaymentRepository;
import com.ridelink.payment.repository.ReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReceiptRepository receiptRepository;
    private final FareCalculatorService fareCalculatorService;

    public Payment createPendingPayment(PaymentRequest request) {
        // 1. Reject invalid payment details before creating a record.
        validateRequest(request);
        // 2. Build and save the payment with PENDING status for later processing.
        return paymentRepository.save(buildPayment(request, PaymentStatus.PENDING));
    }

    public Payment processPayment(PaymentRequest request) {
        // 1. Validate the request before evaluating the payment method.
        validateRequest(request);
        rejectDuplicateSuccessfulPayment(request.rideId());
        // 2. Demo failure methods produce FAILED; all other methods produce SUCCESS.
        PaymentStatus status = isFailureSimulation(request.paymentMethod())
            ? PaymentStatus.FAILED
            : PaymentStatus.SUCCESS;

        // 3. Create and persist the payment with its resulting status.
        Payment payment = buildPayment(request, status);

        payment = paymentRepository.save(payment);

        // 4. Issue a receipt only when the payment succeeded.
        if (status == PaymentStatus.SUCCESS) {
            generateReceipt(payment);
        }

        return payment;
    }

    public Payment processPendingPayment(String rideId) {
        // 1. Find the newest pending payment for this ride, or fail if none exists.
        Payment payment = paymentRepository
                .findFirstByRideIdAndStatusOrderByTimestampDesc(rideId, PaymentStatus.PENDING)
                .orElseThrow(() -> new IllegalStateException("No pending payment exists for ride ID: " + rideId));

            // 2. Determine the outcome from the stored payment method and update its status.
        PaymentStatus status = isFailureSimulation(payment.getPaymentMethod())
                ? PaymentStatus.FAILED
                : PaymentStatus.SUCCESS;
        payment.setStatus(status);
        // 3. Persist the status change.
        payment = paymentRepository.save(payment);

        // 4. Issue a receipt only after successful processing.
        if (status == PaymentStatus.SUCCESS) {
            generateReceipt(payment);
        }

        return payment;
    }

    private Payment buildPayment(PaymentRequest request, PaymentStatus status) {
        // Calculate the fare from distances, then add service-managed status and issue details.
        LocalDateTime issuedAt = LocalDateTime.now();
        return Payment.builder()
            .user(request.user())
                .rideId(request.rideId())
                .passengerId(request.passengerId())
            .amount(calculateTotalFare(request))
                .paymentMethod(request.paymentMethod())
                .status(status)
            .issuedBy("RideLink System")
            .issuedAt(issuedAt)
            .timestamp(issuedAt)
                .build();
    }

        private double calculateTotalFare(PaymentRequest request) {
        // Use measured distance when present; otherwise use the entered distance as the final distance.
        return request.actualDistanceInKm() == null
            ? fareCalculatorService.calculateFinalFare(request.distanceInKm(), request.isPeakHour())
            : fareCalculatorService.calculateAdjustedFare(
                request.distanceInKm(), request.actualDistanceInKm(), request.isPeakHour());
        }

    private void generateReceipt(Payment payment) {
        // Build a receipt linked to the saved payment and its ride.
        Receipt receipt = Receipt.builder()
            .user(payment.getUser())
                .paymentId(payment.getId())
                .rideId(payment.getRideId())
                .totalFare(payment.getAmount())
                .issuedBy("RideLink System")
                .issuedAt(LocalDateTime.now())
                .build();
        
            // Store the generated receipt for later retrieval.
        receiptRepository.save(receipt);
    }

    public Receipt getReceiptByRideId(String rideId) {
            // Return the ride's receipt, or report a not-found error when it does not exist.
        return receiptRepository.findByRideId(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found for ride ID: " + rideId));
    }

    public Payment getPaymentByRideId(String rideId) {
        // Return the newest payment for the ride, or report a not-found error.
        return paymentRepository.findFirstByRideIdOrderByTimestampDesc(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for ride ID: " + rideId));
    }

    private boolean isFailureSimulation(PaymentMethod paymentMethod) {
        // These explicit demo methods make payment failures reproducible.
        return paymentMethod == PaymentMethod.FAIL || paymentMethod == PaymentMethod.DECLINED;
    }

    private void rejectDuplicateSuccessfulPayment(String rideId) {
        if (paymentRepository.existsByRideIdAndStatus(rideId, PaymentStatus.SUCCESS)) {
            throw new IllegalStateException("A successful payment already exists for ride ID: " + rideId);
        }
    }

    private void validateRequest(PaymentRequest request) {
        // Reject missing identity/ride details or an unspecified payment method before calculating the fare.
        if (request == null || isBlank(request.user()) || isBlank(request.rideId()) || isBlank(request.passengerId())
            || request.distanceInKm() == null || !Double.isFinite(request.distanceInKm())
            || request.distanceInKm() <= 0
            || (request.actualDistanceInKm() != null
                && (!Double.isFinite(request.actualDistanceInKm()) || request.actualDistanceInKm() <= 0))
                || request.paymentMethod() == null) {
            throw new IllegalArgumentException("Payment request contains invalid values");
        }
    }

    private boolean isBlank(String value) {
        // Treat both null and whitespace-only identifiers as blank.
        return value == null || value.isBlank();
    }
}