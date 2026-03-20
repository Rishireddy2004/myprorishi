package com.ridesharing.service;

import com.ridesharing.common.exception.ConflictException;
import com.ridesharing.common.exception.UnprocessableEntityException;
import com.ridesharing.domain.User;
import com.ridesharing.dto.RegisterRequestDTO;
import com.ridesharing.repository.UserRepository;
import com.ridesharing.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private JavaMailSender mailSender;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtTokenProvider, redisTemplate, mailSender);
        ReflectionTestUtils.setField(authService, "jwtSecret", "test-secret-key-for-unit-tests-only");
        ReflectionTestUtils.setField(authService, "mailFrom", "noreply@ridesharing.com");
    }

    // ---- register ----

    @Test
    void register_newEmail_savesUserWithHashedPassword() {
        RegisterRequestDTO dto = buildRegisterDto("alice@example.com", "password123", "Alice", "+1234567890");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("$2a$12$hashed");
        User saved = buildUser("alice@example.com", "$2a$12$hashed", "Alice", "+1234567890");
        when(userRepository.save(any(User.class))).thenReturn(saved);

        User result = authService.register(dto);

        assertThat(result.getEmail()).isEqualTo("alice@example.com");
        assertThat(result.getPasswordHash()).isEqualTo("$2a$12$hashed");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateEmail_throwsConflictException() {
        RegisterRequestDTO dto = buildRegisterDto("alice@example.com", "password123", "Alice", "+1234567890");
        when(userRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(buildUser("alice@example.com", "hash", "Alice", "+1234567890")));

        assertThatThrownBy(() -> authService.register(dto))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_passwordIsNeverStoredInPlaintext() {
        RegisterRequestDTO dto = buildRegisterDto("bob@example.com", "mypassword", "Bob", "+9876543210");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("mypassword")).thenReturn("$2a$12$differenthash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = authService.register(dto);

        assertThat(result.getPasswordHash()).isNotEqualTo("mypassword");
    }

    // ---- login ----

    @Test
    void login_validCredentials_returnsToken() {
        User user = buildUser("alice@example.com", "$2a$12$hashed", "Alice", "+1234567890");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "$2a$12$hashed")).thenReturn(true);
        when(jwtTokenProvider.generateToken("alice@example.com", "PASSENGER")).thenReturn("jwt.token.here");

        String token = authService.login("alice@example.com", "password123");

        assertThat(token).isEqualTo("jwt.token.here");
    }

    @Test
    void login_wrongEmail_throwsUnprocessableWithIdenticalMessage() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("unknown@example.com", "anypassword"))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void login_wrongPassword_throwsUnprocessableWithIdenticalMessage() {
        User user = buildUser("alice@example.com", "$2a$12$hashed", "Alice", "+1234567890");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "$2a$12$hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("alice@example.com", "wrongpassword"))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void login_wrongEmailAndWrongPassword_sameErrorMessage() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());
        User user = buildUser("alice@example.com", "$2a$12$hashed", "Alice", "+1234567890");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "$2a$12$hashed")).thenReturn(false);

        String msgWrongEmail = null;
        String msgWrongPassword = null;
        try {
            authService.login("unknown@example.com", "anypassword");
        } catch (UnprocessableEntityException e) {
            msgWrongEmail = e.getMessage();
        }
        try {
            authService.login("alice@example.com", "wrongpassword");
        } catch (UnprocessableEntityException e) {
            msgWrongPassword = e.getMessage();
        }

        assertThat(msgWrongEmail).isNotNull();
        assertThat(msgWrongPassword).isNotNull();
        assertThat(msgWrongEmail).isEqualTo(msgWrongPassword);
    }

    // ---- logout ----

    @Test
    void logout_validToken_addsToBlocklist() {
        when(jwtTokenProvider.getRemainingTtlMs("valid.token")).thenReturn(3600000L);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        authService.logout("valid.token");

        verify(valueOps).set(eq("jwt:blocklist:valid.token"), eq("true"), eq(3600000L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void logout_expiredToken_doesNotAddToBlocklist() {
        when(jwtTokenProvider.getRemainingTtlMs("expired.token")).thenReturn(0L);

        authService.logout("expired.token");

        verify(redisTemplate, never()).opsForValue();
    }

    // ---- requestPasswordReset ----

    @Test
    void requestPasswordReset_existingEmail_storesTokenAndSendsEmail() {
        User user = buildUser("alice@example.com", "$2a$12$hashed", "Alice", "+1234567890");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        authService.requestPasswordReset("alice@example.com");

        verify(valueOps).set(
                startsWith("pwd-reset:alice@example.com"),
                anyString(),
                eq(60L),
                eq(TimeUnit.MINUTES)
        );
        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mailCaptor.capture());
        assertThat(mailCaptor.getValue().getTo()).contains("alice@example.com");
    }

    @Test
    void requestPasswordReset_unknownEmail_doesNotSendEmail() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        authService.requestPasswordReset("unknown@example.com");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    // ---- confirmPasswordReset ----

    @Test
    void confirmPasswordReset_validToken_updatesPassword() {
        User user = buildUser("alice@example.com", "$2a$12$oldhash", "Alice", "+1234567890");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("pwd-reset:alice@example.com")).thenReturn("valid-token");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpassword1")).thenReturn("$2a$12$newhash");

        authService.confirmPasswordReset("alice@example.com", "valid-token", "newpassword1");

        assertThat(user.getPasswordHash()).isEqualTo("$2a$12$newhash");
        verify(userRepository).save(user);
        verify(redisTemplate).delete("pwd-reset:alice@example.com");
    }

    @Test
    void confirmPasswordReset_invalidToken_throwsUnprocessable() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("pwd-reset:alice@example.com")).thenReturn("correct-token");

        assertThatThrownBy(() -> authService.confirmPasswordReset("alice@example.com", "wrong-token", "newpassword1"))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("invalid or has expired");
    }

    @Test
    void confirmPasswordReset_expiredToken_throwsUnprocessable() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("pwd-reset:alice@example.com")).thenReturn(null);

        assertThatThrownBy(() -> authService.confirmPasswordReset("alice@example.com", "any-token", "newpassword1"))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("invalid or has expired");
    }

    // ---- BCrypt hash verification ----

    @Test
    void register_bcryptHashVerification_passwordMatchesStoredHash() {
        // Use real BCrypt encoder to verify the hash is verifiable
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder realEncoder =
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        String rawPassword = "mySecurePassword";
        String hash = realEncoder.encode(rawPassword);

        assertThat(realEncoder.matches(rawPassword, hash)).isTrue();
        assertThat(realEncoder.matches("wrongPassword", hash)).isFalse();
    }

    // ---- JWT expiry boundary ----

    @Test
    void jwtTokenProvider_tokenIssuedNow_expiresWithin24Hours() {
        // 24h in ms = 86_400_000
        long expirationMs = 86_400_000L;
        String secret = "test-secret-key-for-unit-tests-must-be-at-least-32-chars";
        com.ridesharing.security.JwtTokenProvider provider =
                new com.ridesharing.security.JwtTokenProvider(secret, expirationMs);

        String token = provider.generateToken("alice@example.com", "PASSENGER");
        long remainingTtlMs = provider.getRemainingTtlMs(token);

        assertThat(remainingTtlMs).isGreaterThan(0L);
        assertThat(remainingTtlMs).isLessThanOrEqualTo(expirationMs);
    }

    // ---- Reset token TTL boundary ----

    @Test
    void requestPasswordReset_resetTokenStoredWithSixtyMinuteTtl() {
        User user = buildUser("alice@example.com", "$2a$12$hashed", "Alice", "+1234567890");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        authService.requestPasswordReset("alice@example.com");

        verify(valueOps).set(
                startsWith("pwd-reset:alice@example.com"),
                anyString(),
                eq(60L),
                eq(TimeUnit.MINUTES)
        );
    }

    // ---- helpers ----

    private RegisterRequestDTO buildRegisterDto(String email, String password, String fullName, String phone) {
        RegisterRequestDTO dto = new RegisterRequestDTO();
        dto.setEmail(email);
        dto.setPassword(password);
        dto.setFullName(fullName);
        dto.setPhone(phone);
        return dto;
    }

    private User buildUser(String email, String passwordHash, String fullName, String phone) {
        return User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .passwordHash(passwordHash)
                .fullName(fullName)
                .phone(phone)
                .build();
    }
}
