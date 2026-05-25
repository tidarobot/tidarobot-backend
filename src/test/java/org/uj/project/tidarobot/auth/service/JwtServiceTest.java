package org.uj.project.tidarobot.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.uj.project.tidarobot.user.entity.Role;
import org.uj.project.tidarobot.user.entity.Status;
import org.uj.project.tidarobot.user.entity.User;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    // Base64 of "HelloWorldForTestingPurposesOnly1234" — 36 bytes, valid for HS256 (needs >= 32)
    private static final String TEST_SECRET = "SGVsbG9Xb3JsZEZvclRlc3RpbmdQdXJwb3Nlc09ubHkxMjM0";

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET);

        user = User.builder()
                .id(1L)
                .username("testuser")
                .passwordHash("hash")
                .role(Role.USER)
                .status(Status.APPROVED)
                .build();
    }

    @Test
    void generateToken_extractUsername_roundTrip() {
        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUsername(token)).isEqualTo("testuser");
    }

    @Test
    void isTokenValid_validToken_returnsTrue() {
        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void isTokenValid_tokenForDifferentUser_returnsFalse() {
        String token = jwtService.generateToken(user);

        User other = User.builder()
                .username("other")
                .role(Role.USER)
                .status(Status.APPROVED)
                .build();

        assertThat(jwtService.isTokenValid(token, other)).isFalse();
    }

    @Test
    void generateToken_containsRoleClaim() {
        String token = jwtService.generateToken(user);

        // role claim is embedded; extracting it via extractClaim verifies it was set
        String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
        assertThat(role).isEqualTo("USER");
    }
}
