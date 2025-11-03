package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.payload.AuthRequest;
import com.example.demo.payload.AuthResponse;
import com.example.demo.payload.RegisterRequest;
import com.example.demo.payload.VerifyOtpRequest;
import com.example.demo.service.AuthService;
import com.example.demo.repository.UserRepository;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    // ✅ Step 1: Send OTP
    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.sendOtp(request);
    }

    // ✅ Step 2: Verify OTP and create user
    @PostMapping("/verify-otp")
    public AuthResponse verifyOtp(
            @RequestParam String email,
            @RequestBody VerifyOtpRequest request
    ) {
        return authService.verifyOtpAndCreateUser(
                email,
                request.getOtp(),
                request.getUsername(),
                request.getPassword());
    }

    // ✅ Step 3: Login
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest request) {
        return authService.authenticate(request);
    }

    // ✅ Step 4: Get user profile
    @GetMapping("/profile")
    public User getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        // The @AuthenticationPrincipal gives you the currently authenticated user's details
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
