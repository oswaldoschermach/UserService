package com.nebula.userService.dto;

import com.nebula.userService.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Payload para atualizacao de usuario. Envie o conjunto completo de campos mutaveis (`fullName`, `role`, `active`).")
public class UserUpdateDTO {

    @NotBlank(message = "Nome completo é obrigatório")
    @Size(min = 3, max = 100, message = "Nome completo deve ter entre 3 e 100 caracteres")
    @Schema(description = "Novo nome completo do usuario",
            example = "Maria Oliveira",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String fullName;

    @Schema(description = "Novo perfil do usuario",
            example = "ADMIN",
            allowableValues = {"USER", "ADMIN", "MODERATOR"})
    private Role role;

    @Schema(description = "Indica se o usuario deve permanecer ativo",
            example = "true")
    private Boolean active;
}
