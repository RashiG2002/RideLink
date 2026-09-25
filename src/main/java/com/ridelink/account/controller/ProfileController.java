package com.ridelink.account.controller;

import com.ridelink.account.dto.UserResponse;
import com.ridelink.account.service.AccountService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@Tag(
        name = "Profile",
        description = "User profile management APIs"
)
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    private final AccountService accountService;

    public ProfileController(AccountService accountService) {
        this.accountService = accountService;
    }

    @Operation(
            summary = "View my profile",
            description = "Returns the profile of the currently authenticated user"
    )
    @GetMapping
    public UserResponse getMyProfile(
            Authentication authentication
    ) {

        String email = authentication.getName();

        return accountService.getMyProfile(email);
    }

    @Operation(
            summary = "Update my profile",
            description = "Updates the authenticated user's name and phone number"
    )
    @PutMapping
    public UserResponse updateMyProfile(
            Authentication authentication,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String phone
    ) {

        String email = authentication.getName();

        return accountService.updateMyProfile(
                email,
                name,
                phone
        );
    }

    @Operation(
            summary = "Passenger profile access",
            description = "Accessible only to users with PASSENGER role"
    )
    @GetMapping("/passenger")
    @PreAuthorize("hasRole('PASSENGER')")
    public String passengerOnly(
            Authentication authentication
    ) {

        return "Passenger profile access granted for: "
                + authentication.getName();
    }

    @Operation(
            summary = "Driver profile access",
            description = "Accessible only to users with DRIVER role"
    )
    @GetMapping("/driver")
    @PreAuthorize("hasRole('DRIVER')")
    public String driverOnly(
            Authentication authentication
    ) {

        return "Driver profile access granted for: "
                + authentication.getName();
    }
}