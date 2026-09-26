package com.ridelink.payment.exception;

// 1. Use a distinct exception type when a requested resource cannot be found.
public class ResourceNotFoundException extends RuntimeException {

    // 2. Preserve the detail message for the global handler's HTTP 404 response.
    public ResourceNotFoundException(String message) {
        super(message);
    }
}