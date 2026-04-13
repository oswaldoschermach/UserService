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
@Schema(description = "DTO para atualização de usuário")
public class UserUpdateDTO {

    @NotBlank(message = "Nome completo é obrigatório")
    @Size(min = 3, max = 100, message = "Nome completo deve ter entre 3 e 100 caracteres")
    @Schema(description = "Nome completo", example = "Maria Oliveira", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fullName;

    @Schema(description = "Perfil do usuário", example = "ADMIN", allowableValues = {"USER", "ADMIN", "MODERATOR"})
    private Role role;

    @Schema(description = "Status de ativação", example = "true")
    private Boolean active;
}