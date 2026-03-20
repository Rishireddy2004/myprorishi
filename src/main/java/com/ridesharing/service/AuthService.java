package com.ridesharing.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ridesharing.common.exception.ConflictException;
import com.ridesharing.common.exception.ExternalServiceException;
import com.ridesharing.common.exception.UnprocessableEntityException;
import com.ridesharing.domain.User;
import com.ridesharing.dto.RegisterRequestDTO;
import com.ridesharing.repository.UserRepository;
import com.ridesharing.security.JwtTokenProvider;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AuthService {

    private static final String INVALID_CREDENTIALS_MSG = "Invalid credentials";
    private static final String BLOCKLIST_KEY_PREFIX = "jwt:blocklist:";
    private static final String PWD_RESET_KEY_PREFIX = "pwd-reset:";
    private static final long PWD_RESET_TTL_MINUTES = 60L;

    private final Map<String, String> localTokenStore = new ConcurrentHashMap<>();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    @Nullable
    private final StringRedisTemplate redisTemplate;
    private final JavaMailSender mailSender;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       @Nullable @Autowired(required = false) StringRedisTemplate redisTemplate,
                       JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTemplate = redisTemplate;
        this.mailSender = mailSender;
    }

    public User register(RegisterRequestDTO dto) {
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new ConflictException("EMAIL_ALREADY_EXISTS",
                    "An account with this email address already exists.");
        }
        if (dto.getPhone() != null && !dto.getPhone().isBlank()
                && userRepository.findByPhone(dto.getPhone()).isPresent()) {
            throw new ConflictException("PHONE_ALREADY_EXISTS",
                    "An account with this phone number already exists.");
        }
        User user = User.builder()
                .email(dto.getEmail())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .fullName(dto.getFullName())
                .phone(dto.getPhone())
                .role(dto.getRole() != null ? dto.getRole() : "PASSENGER")
                .build();
        return userRepository.save(user);
    }

    public String login(String email, String password) {
        User user = loginUser(email, password);
        return jwtTokenProvider.generateToken(user.getEmail(), user.getRole());
    }

    public User loginUser(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new UnprocessableEntityException("INVALID_CREDENTIALS", INVALID_CREDENTIALS_MSG);
        }
        User user = userOpt.get();
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new UnprocessableEntityException("INVALID_CREDENTIALS", INVALID_CREDENTIALS_MSG);
        }
        if (user.isSuspended()) {
            throw new UnprocessableEntityException("ACCOUNT_SUSPENDED", "Your account has been suspended.");
        }
        return user;
    }

    public String generateToken(User user) {
        return jwtTokenProvider.generateToken(user.getEmail(), user.getRole());
    }

    public void logout(String token) {
        if (redisTemplate == null) return;
        long remainingTtlMs = jwtTokenProvider.getRemainingTtlMs(token);
        if (remainingTtlMs > 0) {
            redisTemplate.opsForValue().set(
                    BLOCKLIST_KEY_PREFIX + token, "true", remainingTtlMs, TimeUnit.MILLISECONDS);
        }
    }

    // ---- Email password reset ----

    public void requestPasswordReset(String email) {
        // Silently do nothing if email not registered (don't leak existence)
        if (userRepository.findByEmail(email).isEmpty()) return;

        String token = generateHmacToken(email);
        storeValue(PWD_RESET_KEY_PREFIX + email, token, PWD_RESET_TTL_MINUTES);

        if (mailFrom == null || mailFrom.isBlank()) {
            // No mail configured — log token so admin/dev can share it manually
            log.warn("=== PASSWORD RESET TOKEN (email not configured) for {} === {}", email, token);
            throw new ExternalServiceException("MAIL_NOT_CONFIGURED",
                    "Email service is not configured on this server. Please contact the administrator to reset your password.");
        }

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(mailFrom);
            msg.setTo(email);
            msg.setSubject("RideShare — Password Reset Token");
            msg.setText("Hello,\n\nYour password reset token (valid 60 minutes):\n\n"
                    + token
                    + "\n\nPaste this token on the Reset Password page to set a new password."
                    + "\n\nIf you did not request this, ignore this email.\n\n— RideShare Team");
            mailSender.send(msg);
            log.info("Password reset email sent to {}", email);
        } catch (MailException ex) {
            log.error("Failed to send reset email to {}: {}", email, ex.getMessage());
            throw new ExternalServiceException("MAIL_SEND_FAILED",
                    "Could not send the reset email. Please check that MAIL_USERNAME and MAIL_PASSWORD are set correctly and restart the server.");
        }
    }

    public void confirmPasswordReset(String email, String token, String newPassword) {
        String stored = getValue(PWD_RESET_KEY_PREFIX + email);
        if (stored == null || !stored.equals(token)) {
            throw new UnprocessableEntityException("INVALID_RESET_TOKEN",
                    "The reset token is invalid or has expired. Please request a new one.");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnprocessableEntityException("USER_NOT_FOUND",
                        "No account found for this email."));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        removeValue(PWD_RESET_KEY_PREFIX + email);
        log.info("Password reset successfully for {}", email);
    }

    // ---- Direct reset (verify by full name — no email/token needed) ----

    public void directReset(String email, String fullName, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnprocessableEntityException("INVALID_IDENTITY",
                        "Email address or full name does not match our records."));
        // Case-insensitive full name check as identity verification
        if (!user.getFullName().trim().equalsIgnoreCase(fullName.trim())) {
            throw new UnprocessableEntityException("INVALID_IDENTITY",
                    "Email address or full name does not match our records.");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Direct password reset for {}", email);
    }

    // ---- Helpers ----

    private void storeValue(String key, String value, long ttlMinutes) {
        if (redisTemplate != null) {
            redisTemplate.opsForValue().set(key, value, ttlMinutes, TimeUnit.MINUTES);
        } else {
            localTokenStore.put(key, value);
        }
    }

    private String getValue(String key) {
        return redisTemplate != null ? redisTemplate.opsForValue().get(key) : localTokenStore.get(key);
    }

    private void removeValue(String key) {
        if (redisTemplate != null) redisTemplate.delete(key);
        else localTokenStore.remove(key);
    }

    private String generateHmacToken(String email) {
        try {
            String payload = email + ":" + System.currentTimeMillis();
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes)
                    + "." + Base64.getUrlEncoder().withoutPadding()
                            .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate reset token", e);
        }
    }
}
