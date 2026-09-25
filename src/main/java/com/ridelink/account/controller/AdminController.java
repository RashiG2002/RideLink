package com.ridelink.account.controller;

import com.ridelink.account.dto.UserResponse;
import com.ridelink.account.service.AccountService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(
        name = "Admin",
        description = "Administrator account management APIs"
)
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AccountService accountService;

    public AdminController(AccountService accountService) {
        this.accountService = accountService;
    }

    @Operation(
            summary = "Get all users",
            description = "Returns all registered users. Accessible only to administrators."
    )
    @GetMapping("/users")
    public List<UserResponse> getAllUsers() {

        return accountService.getAllUsers();
    }

    @Operation(
            summary = "Update user account status",
            description = "Changes a user's status between ACTIVE and INACTIVE"
    )
    @PutMapping("/users/{id}/status")
    public UserResponse updateUserStatus(
            @PathVariable String id,
            @RequestParam String status
    ) {

        return accountService.updateUserStatus(
                id,
                status
        );
    }
}