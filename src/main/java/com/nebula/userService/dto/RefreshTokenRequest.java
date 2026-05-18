package com.nebula.userService.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload para renovar o access token a partir de um refresh token ainda valido e nao revogado")
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token é obrigatório")
    @Schema(description = "Refresh token retornado no endpoint de login",
            example = "eyJhbGciOiJIUzI1NiJ9...",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String refreshToken;
}
