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

    public Payment createPendingPayment(PaymentRequest request) {
        // 1. Reject invalid payment details before creating a record.
        validateRequest(request);
        // 2. Build and save the payment with PENDING status for later processing.
        return paymentRepository.save(buildPayment(request, PaymentStatus.PENDING));
    }

    public Payment processPayment(PaymentRequest request) {
        // 1. Validate the request before evaluating the payment method.
        validateRequest(request);
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
        // Copy the request details and add service-managed status and creation time.
        return Payment.builder()
                .rideId(request.rideId())
                .passengerId(request.passengerId())
                .amount(request.amount())
                .paymentMethod(request.paymentMethod())
                .status(status)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private void generateReceipt(Payment payment) {
        // Build a receipt linked to the saved payment and its ride.
        Receipt receipt = Receipt.builder()
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

    private void validateRequest(PaymentRequest request) {
        // Reject missing identifiers, invalid amounts, or an unspecified payment method.
        if (request == null || isBlank(request.rideId()) || isBlank(request.passengerId())
                || request.amount() == null || !Double.isFinite(request.amount()) || request.amount() <= 0
                || request.paymentMethod() == null) {
            throw new IllegalArgumentException("Payment request contains invalid values");
        }
    }

    private boolean isBlank(String value) {
        // Treat both null and whitespace-only identifiers as blank.
        return value == null || value.isBlank();
    }
}