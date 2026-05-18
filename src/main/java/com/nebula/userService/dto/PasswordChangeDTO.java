package com.nebula.userService.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Payload para troca da propria senha do usuario autenticado")
public class PasswordChangeDTO {

    @NotBlank(message = "Senha atual é obrigatória")
    @Schema(description = "Senha atual em texto puro",
            example = "Senha@123",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String currentPassword;

    @NotBlank(message = "Nova senha é obrigatória")
    @Size(min = 8, message = "A nova senha deve ter no mínimo 8 caracteres")
    @Schema(description = "Nova senha em texto puro. Minimo atual: 8 caracteres.",
            example = "NovaSenha@456",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;
}
