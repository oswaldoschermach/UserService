package com.nebula.userService.configs;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Component
public class JwtConfig {

    private final SecretKey secretKey;
    private final long expirationTime;
    private final long refreshExpirationTime;

    public JwtConfig(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms:86400000}") long expirationTime,
            @Value("${jwt.refresh-expiration-ms:604800000}") long refreshExpirationTime) {
        this.secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
        this.expirationTime = expirationTime;
        this.refreshExpirationTime = refreshExpirationTime;
    }

    public SecretKey getSecretKey() {
        return secretKey;
    }

    public long getExpirationTime() {
        return expirationTime;
    }

    public long getRefreshExpirationTime() {
        return refreshExpirationTime;
    }

    /**
     * Generates an access token (short-lived) with roles.
     */
    public String generateToken(String username) {
        return generateToken(username, List.of());
    }

    public String generateToken(String username, List<String> roles) {
        return Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .claim("type", "access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Generates a refresh token (long-lived, no roles).
     */
    public String generateRefreshToken(String username) {
        return Jwts.builder()
                .subject(username)
                .claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpirationTime))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Parses and returns the claims from any token.
     */
    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Checks if the token is of the expected type ("access" or "refresh").
     */
    public boolean isTokenType(String token, String expectedType) {
        Claims claims = extractClaims(token);
        return expectedType.equals(claims.get("type", String.class));
    }
}