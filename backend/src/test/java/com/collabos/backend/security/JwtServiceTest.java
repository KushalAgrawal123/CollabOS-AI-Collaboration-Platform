package com.collabos.backend.security;

import com.collabos.backend.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "test-only-secret-key-at-least-32-bytes-long!!";

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 60_000);
        user = new User();
        user.setId(42L);
        user.setEmail("jane@example.com");
        user.setName("Jane Doe");
    }

    @Test
    void generatedTokenParsesBackToTheSameIdentity() {
        String token = jwtService.generateToken(user);

        Optional<JwtService.AuthenticatedUser> parsed = jwtService.parse(token);

        assertThat(parsed).isPresent();
        assertThat(parsed.get().id()).isEqualTo(42L);
        assertThat(parsed.get().email()).isEqualTo("jane@example.com");
        assertThat(parsed.get().name()).isEqualTo("Jane Doe");
    }

    @Test
    void expiredTokenFailsToParse() {
        // A negative expiration puts the "expiry" timestamp in the past immediately.
        JwtService expiredIssuer = new JwtService(SECRET, -1000);
        String token = expiredIssuer.generateToken(user);

        assertThat(jwtService.parse(token)).isEmpty();
    }

    @Test
    void tokenSignedWithADifferentSecretIsRejected() {
        JwtService otherIssuer = new JwtService("a-completely-different-secret-key-32bytes!!", 60_000);
        String token = otherIssuer.generateToken(user);

        assertThat(jwtService.parse(token)).isEmpty();
    }

    @Test
    void garbageInputDoesNotThrow() {
        assertThat(jwtService.parse("not-a-real-jwt-at-all")).isEmpty();
    }
}
