package com.ridelink.payment.repository;

import com.ridelink.payment.model.Payment;
import com.ridelink.payment.model.PaymentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface PaymentRepository extends MongoRepository<Payment, String> {
    // 1. Find a payment for the ride, sort newest first, and return the first match if present.
    Optional<Payment> findFirstByRideIdOrderByTimestampDesc(String rideId);

    // 2. Apply both ride and status filters, then return the newest matching payment if present.
    Optional<Payment> findFirstByRideIdAndStatusOrderByTimestampDesc(String rideId, PaymentStatus status);
}