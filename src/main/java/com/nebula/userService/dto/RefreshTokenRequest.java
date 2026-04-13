package com.nebula.userService.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Requisição com refresh token para renovação do access token")
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token é obrigatório")
    @Schema(description = "Refresh token recebido no login", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String refreshToken;
}
