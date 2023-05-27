package com.cancerrisk.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

class JwtServiceTest {

    private static final JwtProperties PROPS = new JwtProperties(
            "unit-test-secret-that-is-long-enough-for-hs256", "test-issuer", Duration.ofMinutes(15), Duration.ofDays(7));
    private static final UserDetails USER = User.withUsername("doc@example.com").password("x").roles("DOCTOR").build();

    @Test
    void accessTokenRoundTrip() {
        JwtService jwt = new JwtService(PROPS);
        String token = jwt.generateAccessToken(USER);
        assertThat(jwt.validate(token, JwtService.ACCESS)).contains("doc@example.com");
        assertThat(jwt.validate(token, JwtService.REFRESH)).isEmpty();
    }

    @Test
    void expiredTokenIsRejected() {
        Instant issued = Instant.parse("2023-03-01T10:00:00Z");
        String token = new JwtService(PROPS, Clock.fixed(issued, ZoneOffset.UTC)).generateAccessToken(USER);
        JwtService later = new JwtService(PROPS, Clock.fixed(issued.plus(Duration.ofMinutes(16)), ZoneOffset.UTC));
        assertThat(later.validate(token, JwtService.ACCESS)).isEmpty();
    }

    @Test
    void tokenSignedWithOtherKeyIsRejected() {
        JwtService other = new JwtService(new JwtProperties(
                "a-completely-different-secret-for-signing-tokens", "test-issuer", Duration.ofMinutes(5), Duration.ofDays(1)));
        assertThat(new JwtService(PROPS).validate(other.generateAccessToken(USER), JwtService.ACCESS)).isEmpty();
    }

    @Test
    void garbageIsRejected() {
        assertThat(new JwtService(PROPS).validate("not-a-jwt", JwtService.ACCESS)).isEmpty();
    }
}
