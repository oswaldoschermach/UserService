package com.nebula.userService.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload para solicitar recuperacao de senha. A API sempre retorna 204 para evitar enumeracao de usuarios.")
public class PasswordResetRequestDTO {

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ser válido")
    @Schema(description = "E-mail cadastrado na conta que deve receber o token de recuperacao",
            example = "joao@empresa.com",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;
}
