package com.nebula.userService.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Credenciais de login")
public class LoginRequest {

    @NotBlank(message = "Username é obrigatório")
    @Schema(description = "Nome de usuário", example = "joao.silva", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "Senha é obrigatória")
    @Schema(description = "Senha do usuário", example = "Senha@123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}