package com.ridelink.payment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/** A receipt document created after a payment succeeds. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "receipts")
public class Receipt {
    // 1. Database identifier for this receipt document.
    @Id
    private String id;

    // 2. Identify the user and ride documented by this receipt.
    private String user;
    private String paymentId;
    private String rideId;

    // 3. Record the fare total shown on the receipt.
    private Double totalFare;

    // 4. Record who issued the receipt and when it was created.
    private String issuedBy;
    private LocalDateTime issuedAt;
}