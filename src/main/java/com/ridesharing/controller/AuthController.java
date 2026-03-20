package com.ridesharing.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ridesharing.domain.User;
import com.ridesharing.dto.LoginRequestDTO;
import com.ridesharing.dto.RegisterRequestDTO;
import com.ridesharing.service.AuthService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** POST /auth/register — 201 Created */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequestDTO dto) {
        User user = authService.register(dto);
        Map<String, Object> body = Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "fullName", user.getFullName(),
                "phone", user.getPhone() != null ? user.getPhone() : "",
                "role", user.getRole()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    /** POST /auth/login — 200 with JWT */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequestDTO dto) {
        User user = authService.loginUser(dto.getEmail(), dto.getPassword());
        String token = authService.generateToken(user);
        Map<String, Object> body = Map.of(
                "token", token,
                "id", user.getId(),
                "name", user.getFullName(),
                "role", user.getRole()
        );
        return ResponseEntity.ok(body);
    }

    /** POST /auth/logout — 200 (requires Authorization header) */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());
            authService.logout(token);
        }
        return ResponseEntity.ok().build();
    }

    /** POST /auth/password-reset/request — 200 */
    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequestBody body) {
        authService.requestPasswordReset(body.email());
        return ResponseEntity.ok().build();
    }

    /** POST /auth/password-reset/confirm — 200 */
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmBody body) {
        authService.confirmPasswordReset(body.email(), body.token(), body.newPassword());
        return ResponseEntity.ok().build();
    }

    /** POST /auth/password-reset/direct — no email/token, verified by full name */
    @PostMapping("/password-reset/direct")
    public ResponseEntity<Void> directReset(@Valid @RequestBody DirectResetBody body) {
        authService.directReset(body.email(), body.fullName(), body.newPassword());
        return ResponseEntity.ok().build();
    }

    // ---- Inner record types for request bodies ----

    record PasswordResetRequestBody(
            @NotNull @Email String email
    ) {}

    record PasswordResetConfirmBody(
            @NotNull @Email String email,
            @NotNull @NotBlank String token,
            @NotNull @Size(min = 8) String newPassword
    ) {}

    record DirectResetBody(
            @NotNull @Email String email,
            @NotNull @NotBlank String fullName,
            @NotNull @Size(min = 8) String newPassword
    ) {}
}
