package com.example.demo.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import java.time.Duration;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final StringRedisTemplate redisTemplate;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine, StringRedisTemplate redisTemplate) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.redisTemplate = redisTemplate;
    }

    public void sendOtpEmail(String toEmail, String otp) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        String redisKey = "otp_rate_limit:" + toEmail;

        // 🔒 Check rate limit (1 OTP per minute)
        if (ops.get(redisKey) != null) {
            throw new RuntimeException("OTP already sent recently. Try again after 1 minute.");
        }

        try {
            // Create email context for Thymeleaf
            Context context = new Context();
            context.setVariable("otp", otp);

            // Generate HTML content from Thymeleaf template
            String htmlContent = templateEngine.process("otp-email", context);

            // Create MIME message
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(toEmail);
            helper.setSubject("Your OTP Code");
            helper.setText(htmlContent, true);

            // Send email
            mailSender.send(message);

            // ⏱️ Store rate limit key in Redis (expire in 60 seconds)
            ops.set(redisKey, "sent", Duration.ofSeconds(60));

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }
}
