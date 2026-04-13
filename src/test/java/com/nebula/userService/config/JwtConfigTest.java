package com.nebula.userService.config;

import com.nebula.userService.configs.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtConfig Tests")
class JwtConfigTest {

    private JwtConfig jwtConfig;
    private static final String SECRET = "dGVzdFNlY3JldEtleUZvckp3dEF1dGgxMjM0NTY3ODk=";
    private static final long EXPIRATION_MS = 3600000L;

    @BeforeEach
    void setUp() {
        jwtConfig = new JwtConfig(SECRET, EXPIRATION_MS, 604800000L);
    }

    @Test
    @DisplayName("generateToken - retorna token valido")
    void generateToken_ReturnsValidToken() {
        String token = jwtConfig.generateToken("joao.silva");
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("generateToken - com roles retorna token valido")
    void generateToken_WithRoles_ReturnsValidToken() {
        String token = jwtConfig.generateToken("joao.silva", List.of("ROLE_USER", "ROLE_ADMIN"));
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("extractClaims - retorna subject correto")
    void extractClaims_ReturnsCorrectSubject() {
        String token = jwtConfig.generateToken("joao.silva", List.of("ROLE_USER"));
        Claims claims = jwtConfig.extractClaims(token);
        assertThat(claims.getSubject()).isEqualTo("joao.silva");
    }

    @Test
    @DisplayName("extractClaims - retorna roles corretas")
    void extractClaims_ReturnsRoles() {
        String token = jwtConfig.generateToken("joao.silva", List.of("ROLE_USER"));
        Claims claims = jwtConfig.extractClaims(token);
        List<?> roles = claims.get("roles", List.class);
        assertThat(roles).hasSize(1);
        assertThat(roles.get(0)).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("extractClaims - token expirado lanca ExpiredJwtException")
    void extractClaims_ExpiredToken_ThrowsExpiredJwtException() {
        JwtConfig shortLived = new JwtConfig(SECRET, 1L, 604800000L);
        String token = shortLived.generateToken("joao.silva");
        try {
            Thread.sleep(50);
        } catch (InterruptedException ignored) {
        }
        assertThatThrownBy(() -> shortLived.extractClaims(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("extractClaims - token invalido lanca excecao")
    void extractClaims_InvalidToken_ThrowsException() {
        assertThatThrownBy(() -> jwtConfig.extractClaims("token.invalido.assinatura"))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("getSecretKey - nao deve ser nulo")
    void getSecretKey_NotNull() {
        assertThat(jwtConfig.getSecretKey()).isNotNull();
    }

    @Test
    @DisplayName("getExpirationTime - retorna o valor configurado")
    void getExpirationTime_ReturnsConfiguredValue() {
        assertThat(jwtConfig.getExpirationTime()).isEqualTo(EXPIRATION_MS);
    }

    @Test
    @DisplayName("generateToken - usuarios diferentes geram tokens diferentes")
    void generateToken_DifferentUsers_DifferentTokens() {
        String token1 = jwtConfig.generateToken("user1");
        String token2 = jwtConfig.generateToken("user2");
        assertThat(token1).isNotEqualTo(token2);
    }
}