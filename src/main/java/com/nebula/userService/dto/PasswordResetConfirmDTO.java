package com.nebula.userService.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload para redefinir a senha com o token recebido por e-mail")
public class PasswordResetConfirmDTO {

    @NotBlank(message = "Token é obrigatório")
    @Schema(description = "Token bruto enviado por e-mail. Validade atual: 30 minutos.",
            example = "550e8400-e29b-41d4-a716-446655440000",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String token;

    @NotBlank(message = "Nova senha é obrigatória")
    @Size(min = 8, message = "A nova senha deve ter no mínimo 8 caracteres")
    @Schema(description = "Nova senha em texto puro. Minimo atual: 8 caracteres.",
            example = "NovaSenha@456",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;
}
