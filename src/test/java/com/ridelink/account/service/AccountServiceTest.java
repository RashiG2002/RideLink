package com.ridelink.account.service;

import com.ridelink.account.dto.AuthResponse;
import com.ridelink.account.dto.LoginRequest;
import com.ridelink.account.dto.RegisterRequest;
import com.ridelink.account.dto.UserResponse;
import com.ridelink.account.model.User;
import com.ridelink.account.repository.UserRepository;
import com.ridelink.account.security.JwtService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AccountService accountService;

    private User testUser;

    @BeforeEach
    void setUp() {

        testUser = new User();

        testUser.setId("user123");
        testUser.setName("Test User");
        testUser.setEmail("test@gmail.com");
        testUser.setPassword("encodedPassword");
        testUser.setRole("PASSENGER");
        testUser.setPhone("0771234567");
        testUser.setStatus("ACTIVE");
    }

    // ==========================================
    // TEST 1 - REGISTER SUCCESS
    // ==========================================

    @Test
    void register_shouldCreateUserSuccessfully() {

        RegisterRequest request = new RegisterRequest(
                "Test User",
                "test@gmail.com",
                "password123",
                "PASSENGER",
                "0771234567"
        );

        when(userRepository.existsByEmail("test@gmail.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("password123"))
                .thenReturn("encodedPassword");

        when(userRepository.save(any(User.class)))
                .thenReturn(testUser);

        UserResponse response =
                accountService.register(request);

        assertNotNull(response);

        assertEquals(
                "test@gmail.com",
                response.email()
        );

        assertEquals(
                "PASSENGER",
                response.role()
        );

        verify(userRepository)
                .existsByEmail("test@gmail.com");

        verify(userRepository)
                .save(any(User.class));
    }

    // ==========================================
    // TEST 2 - REGISTER DUPLICATE EMAIL
    // ==========================================

    @Test
    void register_shouldRejectDuplicateEmail() {

        RegisterRequest request = new RegisterRequest(
                "Test User",
                "test@gmail.com",
                "password123",
                "PASSENGER",
                "0771234567"
        );

        when(userRepository.existsByEmail("test@gmail.com"))
                .thenReturn(true);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> accountService.register(request)
                );

        assertEquals(
                "Email already registered",
                exception.getMessage()
        );

        verify(userRepository, never())
                .save(any(User.class));
    }

    // ==========================================
    // TEST 3 - LOGIN SUCCESS
    // ==========================================

    @Test
    void login_shouldReturnJwtToken() {

        LoginRequest request = new LoginRequest(
                "test@gmail.com",
                "password123"
        );

        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(testUser));

        when(passwordEncoder.matches(
                "password123",
                "encodedPassword"
        )).thenReturn(true);

        when(jwtService.generateToken(
                "test@gmail.com",
                "PASSENGER"
        )).thenReturn("jwt-token");

        AuthResponse response =
                accountService.login(request);

        assertNotNull(response);

        assertEquals(
                "jwt-token",
                response.token()
        );

        assertEquals(
                "Bearer",
                response.tokenType()
        );

        assertEquals(
                "test@gmail.com",
                response.email()
        );

        assertEquals(
                "PASSENGER",
                response.role()
        );

        verify(jwtService)
                .generateToken(
                        "test@gmail.com",
                        "PASSENGER"
                );
    }

    // ==========================================
    // TEST 4 - LOGIN INVALID PASSWORD
    // ==========================================

    @Test
    void login_shouldRejectInvalidPassword() {

        LoginRequest request = new LoginRequest(
                "test@gmail.com",
                "wrongPassword"
        );

        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(testUser));

        when(passwordEncoder.matches(
                "wrongPassword",
                "encodedPassword"
        )).thenReturn(false);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> accountService.login(request)
                );

        assertEquals(
                "Invalid email or password",
                exception.getMessage()
        );

        verify(jwtService, never())
                .generateToken(any(), any());
    }

    // ==========================================
    // TEST 5 - LOGIN INACTIVE ACCOUNT
    // ==========================================

    @Test
    void login_shouldRejectInactiveAccount() {

        testUser.setStatus("INACTIVE");

        LoginRequest request = new LoginRequest(
                "test@gmail.com",
                "password123"
        );

        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(testUser));

        when(passwordEncoder.matches(
                "password123",
                "encodedPassword"
        )).thenReturn(true);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> accountService.login(request)
                );

        assertEquals(
                "Account is not active",
                exception.getMessage()
        );

        verify(jwtService, never())
                .generateToken(any(), any());
    }

    // ==========================================
    // TEST 6 - GET ALL USERS
    // ==========================================

    @Test
    void getAllUsers_shouldReturnUsers() {

        User secondUser = new User();

        secondUser.setId("user456");
        secondUser.setName("Driver User");
        secondUser.setEmail("driver@gmail.com");
        secondUser.setPassword("encodedPassword");
        secondUser.setRole("DRIVER");
        secondUser.setPhone("0712345678");
        secondUser.setStatus("ACTIVE");

        when(userRepository.findAll())
                .thenReturn(List.of(
                        testUser,
                        secondUser
                ));

        List<UserResponse> response =
                accountService.getAllUsers();

        assertNotNull(response);

        assertEquals(
                2,
                response.size()
        );

        assertEquals(
                "test@gmail.com",
                response.get(0).email()
        );

        assertEquals(
                "driver@gmail.com",
                response.get(1).email()
        );

        verify(userRepository)
                .findAll();
    }

    // ==========================================
    // TEST 7 - GET MY PROFILE
    // ==========================================

    @Test
    void getMyProfile_shouldReturnUserProfile() {

        when(userRepository.findByEmail(
                "test@gmail.com"
        )).thenReturn(Optional.of(testUser));

        UserResponse response =
                accountService.getMyProfile(
                        "test@gmail.com"
                );

        assertNotNull(response);

        assertEquals(
                "Test User",
                response.name()
        );

        assertEquals(
                "test@gmail.com",
                response.email()
        );

        assertEquals(
                "PASSENGER",
                response.role()
        );
    }

    // ==========================================
    // TEST 8 - UPDATE MY PROFILE
    // ==========================================

    @Test
    void updateMyProfile_shouldUpdateNameAndPhone() {

        when(userRepository.findByEmail(
                "test@gmail.com"
        )).thenReturn(Optional.of(testUser));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        UserResponse response =
                accountService.updateMyProfile(
                        "test@gmail.com",
                        "Updated User",
                        "0711111111"
                );

        assertNotNull(response);

        assertEquals(
                "Updated User",
                response.name()
        );

        assertEquals(
                "0711111111",
                response.phone()
        );

        verify(userRepository)
                .save(any(User.class));
    }

    // ==========================================
    // TEST 9 - UPDATE USER STATUS
    // ==========================================

    @Test
    void updateUserStatus_shouldSetInactive() {

        when(userRepository.findById("user123"))
                .thenReturn(Optional.of(testUser));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        UserResponse response =
                accountService.updateUserStatus(
                        "user123",
                        "INACTIVE"
                );

        assertNotNull(response);

        assertEquals(
                "INACTIVE",
                response.status()
        );

        verify(userRepository)
                .save(any(User.class));
    }

    // ==========================================
    // TEST 10 - INVALID STATUS
    // ==========================================

    @Test
    void updateUserStatus_shouldRejectInvalidStatus() {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> accountService.updateUserStatus(
                                "user123",
                                "BLOCKED"
                        )
                );

        assertEquals(
                "Status must be ACTIVE or INACTIVE",
                exception.getMessage()
        );

        verify(userRepository, never())
                .findById(any());
    }
}