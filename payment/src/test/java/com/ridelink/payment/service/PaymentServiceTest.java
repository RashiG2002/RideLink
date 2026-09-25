package com.ridelink.payment.service;

import com.ridelink.payment.dto.PaymentRequest;
import com.ridelink.payment.exception.ResourceNotFoundException;
import com.ridelink.payment.model.Payment;
import com.ridelink.payment.model.PaymentMethod;
import com.ridelink.payment.model.PaymentStatus;
import com.ridelink.payment.model.Receipt;
import com.ridelink.payment.repository.PaymentRepository;
import com.ridelink.payment.repository.ReceiptRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void successfulPaymentGeneratesReceipt() {
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Payment payment = paymentService.processPayment(
                new PaymentRequest("ride-1", "passenger-1", 800.0, PaymentMethod.CARD));

        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        verify(receiptRepository).save(any(Receipt.class));
    }

    @Test
    void pendingPaymentIsSavedWithoutGeneratingReceipt() {
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Payment payment = paymentService.createPendingPayment(
                new PaymentRequest("ride-pending", "passenger-1", 800.0, PaymentMethod.CARD));

        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        verify(receiptRepository, never()).save(any(Receipt.class));
    }

        @Test
        void pendingPaymentCanBeProcessedAndUpdatedToSuccess() {
                Payment pending = Payment.builder()
                                .rideId("ride-process")
                                .paymentMethod(PaymentMethod.CARD)
                                .status(PaymentStatus.PENDING)
                                .build();
                when(paymentRepository.findFirstByRideIdAndStatusOrderByTimestampDesc(
                                "ride-process", PaymentStatus.PENDING)).thenReturn(Optional.of(pending));
                when(paymentRepository.save(any(Payment.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                Payment payment = paymentService.processPendingPayment("ride-process");

                assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
                verify(receiptRepository).save(any(Receipt.class));
        }

    @Test
    void declinedSimulationIsDeterministicAndDoesNotGenerateReceipt() {
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Payment payment = paymentService.processPayment(
                new PaymentRequest("ride-2", "passenger-1", 800.0, PaymentMethod.DECLINED));

        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        verify(receiptRepository, never()).save(any(Receipt.class));
    }

    @Test
    void retrievesPaymentByRideId() {
        Payment payment = Payment.builder().rideId("ride-1").status(PaymentStatus.SUCCESS).build();
        when(paymentRepository.findFirstByRideIdOrderByTimestampDesc("ride-1"))
                .thenReturn(Optional.of(payment));

        assertEquals(payment, paymentService.getPaymentByRideId("ride-1"));
    }

    @Test
    void missingReceiptReturnsNotFoundError() {
        when(receiptRepository.findByRideId("ride-404")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.getReceiptByRideId("ride-404"));
    }
}
