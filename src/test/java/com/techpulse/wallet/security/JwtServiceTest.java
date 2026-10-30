
package com.techpulse.wallet.security;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 3600000);
    }

    @Test
    void generateTokenAndExtractUsername() {

        String token = jwtService.generateToken(
                1L,
                "test@gmail.com",
                "USER"
        );

        assertNotNull(token);

        String email = jwtService.extractUsername(token);

        assertEquals("test@gmail.com", email);
    }


    @Test
    void isTokenValidWithCorrectEmail() {

        String token = jwtService.generateToken(
                1L,
                "test@gmail.com",
                "USER"
        );

        boolean result = jwtService.isTokenValid(
                token,
                "test@gmail.com"
        );

        assertTrue(result);
    }


    @Test
    void isTokenValidWithIncorrectEmail() {

        String token = jwtService.generateToken(
                1L,
                "test@gmail.com",
                "USER"
        );

        boolean result = jwtService.isTokenValid(
                token,
                "wrong@gmail.com"
        );

        assertFalse(result);
    }

    @Test
    void isTokenValidWithInvalidToken() {

        String invalidToken = "this.is.invalid.token";

        boolean result = jwtService.isTokenValid(
                invalidToken,
                "test@gmail.com"
        );

        assertFalse(result);
    }

    @Test
    void isTokenValidWithExpiredToken() {

        SecretKey key = Keys.hmacShaKeyFor(
                Base64.getDecoder().decode(SECRET)
        );

        Date now = new Date();

        String expiredToken = Jwts.builder()
                .subject("test@gmail.com")
                .issuedAt(new Date(now.getTime() - 10000))
                .expiration(new Date(now.getTime() - 5000))
                .signWith(key)
                .compact();

        boolean result = jwtService.isTokenValid(
                expiredToken,
                "test@gmail.com"
        );

        assertFalse(result);
    }

    @Test
    void extractUsernameWithInvalidToken() {

        String invalidToken = "invalid.jwt.token";

        assertThrows(
                io.jsonwebtoken.JwtException.class,
                () -> jwtService.extractUsername(invalidToken)
        );
    }

}
