package com.ridelink.account.dto;

public record AuthResponse(

        String token,
        String tokenType,
        String id,
        String name,
        String email,
        String role
) {
}