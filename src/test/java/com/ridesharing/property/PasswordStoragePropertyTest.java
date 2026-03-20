package com.ridesharing.property;

// Feature: smart-ride-sharing-system, Property 1: Password is never stored in plaintext

import net.jqwik.api.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property 1: Password is never stored in plaintext.
 * Validates: Requirements 1.4
 */
class PasswordStoragePropertyTest {

    // Use cost factor 4 (minimum) for test speed; production uses 12
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);

    @Property(tries = 100)
    void passwordIsNeverStoredInPlaintext(@ForAll("validPasswords") String plainPassword) {
        // Act: encode the password as AuthService does
        String storedHash = encoder.encode(plainPassword);

        // Assert: stored hash must never equal the plaintext password
        assertThat(storedHash)
                .as("Password hash must not equal plaintext password '%s'", plainPassword)
                .isNotEqualTo(plainPassword);

        // Assert: the stored value must be a valid BCrypt hash (starts with $2a$ or $2b$)
        assertThat(storedHash)
                .as("Stored value must be a BCrypt hash")
                .matches("\\$2[ab]\\$\\d{2}\\$.{53}");

        // Assert: BCrypt can verify the original password against the stored hash
        assertThat(encoder.matches(plainPassword, storedHash))
                .as("BCrypt must be able to verify the original password against the stored hash")
                .isTrue();
    }

    @Provide
    Arbitrary<String> validPasswords() {
        // Passwords: 8–64 chars, printable ASCII excluding space
        return Arbitraries.strings()
                .withCharRange('!', '~')
                .ofMinLength(8)
                .ofMaxLength(64);
    }
}
