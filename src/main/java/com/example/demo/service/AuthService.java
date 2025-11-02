package com.example.demo.service;

import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.payload.AuthRequest;
import com.example.demo.payload.AuthResponse;
import com.example.demo.payload.RegisterRequest;
import com.example.demo.repository.UserRepository;
import com.example.demo.config.JwtService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authManager;
    private final OtpService otpService;
    private final EmailService emailService;
    private final StringRedisTemplate redisTemplate;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authManager,
            OtpService otpService,
            EmailService emailService,
            StringRedisTemplate redisTemplate) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authManager = authManager;
        this.otpService = otpService;
        this.emailService = emailService;
        this.redisTemplate = redisTemplate;
    }

    // ✅ Step 1: Send OTP with Redis rate limiting
    public AuthResponse sendOtp(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already registered");
        }

        // Rate limiting key
        String rateLimitKey = "otp_limit:" + request.getEmail();

        if (redisTemplate.hasKey(rateLimitKey)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "OTP already sent recently. Please wait a minute before retrying.");
        }

        String otp = otpService.generateOtp(request.getEmail());
        emailService.sendOtpEmail(request.getEmail(), otp);

        // Set TTL = 1 minute to limit requests
        redisTemplate.opsForValue().set(rateLimitKey, "sent", Duration.ofMinutes(1));

        return AuthResponse.withMessage("OTP sent to " + request.getEmail());
    }

    // ✅ Step 2: Verify OTP and register user
    public AuthResponse verifyOtpAndCreateUser(String email, String otp, String username, String password) {
        if (!otpService.verifyOtp(email, otp)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired OTP");
        }

        if (userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already registered");
        }

        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(Role.USER);
        userRepository.save(user);

        String token = jwtService.generateToken(user);
        return AuthResponse.withToken(token);
    }

    // ✅ Step 3: Login
    public AuthResponse authenticate(AuthRequest request) {
        var existingUser = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        request.getEmail() + " is not registered"));

        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()));

        String token = jwtService.generateToken(existingUser);
        return AuthResponse.withToken(token);

    }
}
