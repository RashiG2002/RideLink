package com.ridelink.payment.service;

import com.ridelink.payment.dto.PaymentRequest;
import com.ridelink.payment.exception.ResourceNotFoundException;
import com.ridelink.payment.model.Payment;
import com.ridelink.payment.model.PaymentMethod;
import com.ridelink.payment.model.PaymentStatus;
import com.ridelink.payment.model.Receipt;
import com.ridelink.payment.repository.PaymentRepository;
import com.ridelink.payment.repository.ReceiptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ReceiptRepository receiptRepository;

    private PaymentService paymentService;

        @BeforeEach
        void setUp() {
                paymentService = new PaymentService(
                                paymentRepository, receiptRepository, new FareCalculatorService());
        }

    @Test
    void successfulPaymentGeneratesReceipt() {
                // Arrange persistence to return the payment unchanged, then process a normal method.
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Payment payment = paymentService.processPayment(
                paymentRequest("ride-1", "passenger-1", PaymentMethod.CARD));

        // Confirm success and verify that a receipt was stored.
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals(350.0, payment.getAmount());
        assertEquals("user-1", payment.getUser());
        assertEquals("ride-1", payment.getRideId());
        assertEquals("passenger-1", payment.getPassengerId());
        assertEquals("RideLink System", payment.getIssuedBy());
        assertEquals(payment.getTimestamp(), payment.getIssuedAt());
        ArgumentCaptor<Receipt> receiptCaptor = ArgumentCaptor.forClass(Receipt.class);
        verify(receiptRepository).save(receiptCaptor.capture());
        assertEquals(payment.getAmount(), receiptCaptor.getValue().getTotalFare());
        assertEquals(payment.getUser(), receiptCaptor.getValue().getUser());
        assertEquals(payment.getRideId(), receiptCaptor.getValue().getRideId());
        assertEquals("RideLink System", receiptCaptor.getValue().getIssuedBy());
        assertNotNull(receiptCaptor.getValue().getIssuedAt());
    }

    @Test
    void pendingPaymentIsSavedWithoutGeneratingReceipt() {
                // Arrange persistence to return the pending record unchanged.
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Create a pending payment, then confirm no receipt is generated before processing.
        Payment payment = paymentService.createPendingPayment(
                paymentRequest("ride-pending", "passenger-1", PaymentMethod.CARD));

        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertEquals(350.0, payment.getAmount());
        verify(receiptRepository, never()).save(any(Receipt.class));
    }

        @Test
        void pendingPaymentCanBeProcessedAndUpdatedToSuccess() {
                // Arrange a stored pending payment and mock the repository update.
                Payment pending = Payment.builder()
                                .user("user-1")
                                .rideId("ride-process")
                                .passengerId("passenger-1")
                                .amount(350.0)
                                .paymentMethod(PaymentMethod.CARD)
                                .status(PaymentStatus.PENDING)
                                .build();
                when(paymentRepository.findFirstByRideIdAndStatusOrderByTimestampDesc(
                                "ride-process", PaymentStatus.PENDING)).thenReturn(Optional.of(pending));
                when(paymentRepository.save(any(Payment.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                // Process the pending record and verify its successful transition creates a receipt.
                Payment payment = paymentService.processPendingPayment("ride-process");

                assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
                verify(receiptRepository).save(any(Receipt.class));
        }

    @Test
    void declinedSimulationIsDeterministicAndDoesNotGenerateReceipt() {
                // Arrange persistence, then submit the explicit failure-simulation method.
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Payment payment = paymentService.processPayment(
                paymentRequest("ride-2", "passenger-1", PaymentMethod.DECLINED));

        // Confirm the failed status and ensure no receipt was stored.
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        verify(receiptRepository, never()).save(any(Receipt.class));
    }

        @Test
        void duplicateSuccessfulPaymentIsRejected() {
                // Simulate an existing successful payment for this ride.
                when(paymentRepository.existsByRideIdAndStatus("ride-paid", PaymentStatus.SUCCESS))
                                .thenReturn(true);

                // Confirm the duplicate is rejected before either payment or receipt persistence.
                assertThrows(IllegalStateException.class, () -> paymentService.processPayment(
                                paymentRequest("ride-paid", "passenger-1", PaymentMethod.CARD)));
                verify(paymentRepository, never()).save(any(Payment.class));
                verify(receiptRepository, never()).save(any(Receipt.class));
        }

        @Test
        void invalidPaymentRequestIsRejectedBeforePersistence() {
                // Submit a blank ride ID and confirm validation stops the request before saving.
                assertThrows(IllegalArgumentException.class, () -> paymentService.processPayment(
                                new PaymentRequest("user-1", " ", "passenger-1", 5.0, null, false,
                                        PaymentMethod.CARD)));
                verify(paymentRepository, never()).save(any(Payment.class));
        }

    @Test
    void retrievesPaymentByRideId() {
                // Arrange a matching repository result and verify the service returns it.
        Payment payment = Payment.builder().rideId("ride-1").status(PaymentStatus.SUCCESS).build();
        when(paymentRepository.findFirstByRideIdOrderByTimestampDesc("ride-1"))
                .thenReturn(Optional.of(payment));

        assertEquals(payment, paymentService.getPaymentByRideId("ride-1"));
    }

    @Test
    void missingReceiptReturnsNotFoundError() {
                // Arrange an empty lookup result and verify the service reports the missing receipt.
        when(receiptRepository.findByRideId("ride-404")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.getReceiptByRideId("ride-404"));
    }

    @Test
    void missingPendingPaymentReturnsConflictError() {
                // Arrange an empty pending-payment lookup and verify processing cannot continue.
        when(paymentRepository.findFirstByRideIdAndStatusOrderByTimestampDesc(
                "ride-404", PaymentStatus.PENDING)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> paymentService.processPendingPayment("ride-404"));
    }

    @Test
    void pendingDeclinedPaymentBecomesFailedWithoutReceipt() {
                // Arrange a pending payment using the deterministic failure method.
        Payment pending = Payment.builder()
                .user("user-1")
                .rideId("ride-failed")
                .passengerId("passenger-1")
                .amount(350.0)
                .paymentMethod(PaymentMethod.DECLINED)
                .status(PaymentStatus.PENDING)
                .build();
        when(paymentRepository.findFirstByRideIdAndStatusOrderByTimestampDesc(
                "ride-failed", PaymentStatus.PENDING)).thenReturn(Optional.of(pending));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Process it and confirm failure is saved without issuing a receipt.
        Payment payment = paymentService.processPendingPayment("ride-failed");

        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        verify(receiptRepository, never()).save(any(Receipt.class));
    }

        private PaymentRequest paymentRequest(String rideId, String passengerId, PaymentMethod paymentMethod) {
                return new PaymentRequest("user-1", rideId, passengerId, 5.0, null, false, paymentMethod);
        }
}
