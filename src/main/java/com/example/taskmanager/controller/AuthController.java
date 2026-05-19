package com.example.taskmanager.controller;

import com.example.taskmanager.dto.*;
import com.example.taskmanager.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user authentication")
@SecurityRequirement(name = "Keycloak")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register user and send OTP to email")
    public ResponseEntity<String> register(@Valid @RequestBody UserRegistrationDTO registrationDTO) {
        authService.register(registrationDTO);
        return new ResponseEntity<>("Registration initiated. Please check your email for the OTP.", HttpStatus.CREATED);
    }

    @PostMapping("/register/verify")
    @Operation(summary = "Verify OTP and complete registration")
    public ResponseEntity<String> verify(@Valid @RequestBody OtpVerificationRequest verificationRequest) {
        authService.verifyOtp(verificationRequest);
        return ResponseEntity.ok("User registered and verified successfully.");
    }

    @PostMapping("/login")
    @Operation(summary = "Login to get access token")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<TokenResponse> refresh(@RequestBody RefreshTokenRequest refreshRequest) {
        return ResponseEntity.ok(authService.refreshToken(refreshRequest));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user")
    public ResponseEntity<String> logout(@RequestBody RefreshTokenRequest logoutRequest) {
        authService.logout(logoutRequest);
        return ResponseEntity.ok("Logged out successfully");
    }
}
