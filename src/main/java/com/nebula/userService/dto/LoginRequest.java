package com.nebula.userService.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload de login. O campo `username` aceita o nome de usuario cadastrado, nao o e-mail.")
public class LoginRequest {

    @NotBlank(message = "Username é obrigatório")
    @Schema(description = "Nome de usuario unico cadastrado no sistema",
            example = "joao.silva",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "Senha é obrigatória")
    @Schema(description = "Senha em texto puro enviada apenas no login",
            example = "Senha@123",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}
