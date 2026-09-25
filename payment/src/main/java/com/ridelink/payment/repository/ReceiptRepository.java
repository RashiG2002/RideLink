package com.ridelink.payment.repository;

import com.ridelink.payment.model.Receipt;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface ReceiptRepository extends MongoRepository<Receipt, String> {
    // 1. Find the receipt associated with the ride ID.
    // 2. Return an empty Optional when no matching receipt exists.
    Optional<Receipt> findByRideId(String rideId);
}