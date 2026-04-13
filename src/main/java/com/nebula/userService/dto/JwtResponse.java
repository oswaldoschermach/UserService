package com.nebula.userService.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resposta de autenticação contendo os tokens JWT")
public class JwtResponse {

    @Schema(description = "Access token JWT (expira em 24h)", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String token;

    @Schema(description = "Refresh token para renovar o access token (expira em 7 dias)", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String refreshToken;

    /** Construtor de compatibilidade para testes que só usam o access token. */
    public JwtResponse(String token) {
        this.token = token;
    }
}