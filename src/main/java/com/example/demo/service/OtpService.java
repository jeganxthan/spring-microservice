package com.example.demo.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Random;

@Service
public class OtpService {

    private static final String OTP_PREFIX = "otp:";
    private final StringRedisTemplate redisTemplate;

    public OtpService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // Generate and store OTP
    public String generateOtp(String email) {
        email = email.trim().toLowerCase(); // normalize key

        String otp = String.format("%06d", new Random().nextInt(999999));
        String key = OTP_PREFIX + email;

        redisTemplate.opsForValue().set(key, otp, Duration.ofMinutes(5)); // TTL 5 minutes
        System.out.println("✅ OTP generated for " + email + ": " + otp);

        // Check Redis store confirmation
        String test = redisTemplate.opsForValue().get(key);
        System.out.println("🔍 Stored OTP in Redis: " + test);

        return otp;
    }

    // Verify OTP
    public boolean verifyOtp(String email, String otp) {
        email = email.trim().toLowerCase(); // normalize key
        String key = OTP_PREFIX + email;

        String storedOtp = redisTemplate.opsForValue().get(key);
        System.out.println("🔍 OTP retrieved from Redis for " + email + ": " + storedOtp);

        boolean valid = storedOtp != null && storedOtp.equals(otp);

        if (valid) {
            redisTemplate.delete(key);
            System.out.println("✅ OTP verified and deleted for " + email);
        } else {
            System.out.println("❌ Invalid or expired OTP for " + email);
        }

        return valid;
    }
}
