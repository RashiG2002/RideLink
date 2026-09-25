package com.ridelink.account.dto;

import com.ridelink.account.model.User;

public record UserResponse(

        String id,
        String name,
        String email,
        String role,
        String phone,
        String status
) {

    public static UserResponse from(User user) {

        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getPhone(),
                user.getStatus()
        );
    }
}