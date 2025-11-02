package com.example.demo.controller;

import com.example.demo.payload.AuthRequest;
import com.example.demo.payload.AuthResponse;
import com.example.demo.payload.RegisterRequest;
import com.example.demo.payload.VerifyOtpRequest;
import com.example.demo.service.AuthService;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ✅ Step 1: Send OTP
    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.sendOtp(request);
    }

    // ✅ Step 2: Verify OTP and create user
    @PostMapping("/verify-otp")
    public AuthResponse verifyOtp(
            @RequestParam String email, // only email as query param
            @RequestBody VerifyOtpRequest request // OTP, username, password in JSON body
    ) {
        return authService.verifyOtpAndCreateUser(
                email,
                request.getOtp(),
                request.getUsername(),
                request.getPassword());
    }

    // ✅ Step 3: Login after registration
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest request) {
        return authService.authenticate(request);
    }
}
