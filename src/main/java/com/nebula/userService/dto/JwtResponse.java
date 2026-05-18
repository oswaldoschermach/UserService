package com.nebula.userService.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resposta de autenticacao contendo o access token e o refresh token")
public class JwtResponse {

    @Schema(description = "Access token JWT usado nos endpoints protegidos via header Authorization. Expira em 24 horas.",
            example = "eyJhbGciOiJIUzI1NiJ9...")
    private String token;

    @Schema(description = "Refresh token usado para emitir um novo access token sem novo login. Expira em 7 dias.",
            example = "eyJhbGciOiJIUzI1NiJ9...")
    private String refreshToken;

    /** Construtor de compatibilidade para testes que só usam o access token. */
    public JwtResponse(String token) {
        this.token = token;
    }
}
