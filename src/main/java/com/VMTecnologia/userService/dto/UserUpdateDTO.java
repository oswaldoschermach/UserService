package com.VMTecnologia.userService.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "DTO para atualização de usuário")
public class UserUpdateDTO {

    @Schema(description = "Nome completo", example = "Maria Oliveira", required = true)
    @NotBlank(message = "Nome completo é obrigatório")
    private String fullName;

    @Schema(description = "Perfil do usuário", example = "ADMIN", allowableValues = {"USER", "ADMIN"})
    private String role;

    @Schema(description = "Status de ativação", example = "true")
    private Boolean active;
}