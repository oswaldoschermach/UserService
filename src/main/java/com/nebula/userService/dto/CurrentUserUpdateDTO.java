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
@Schema(description = "Payload para atualizacao do proprio perfil do usuario autenticado")
public class CurrentUserUpdateDTO {

    @NotBlank(message = "Nome completo é obrigatório")
    @Size(min = 3, max = 100, message = "Nome completo deve ter entre 3 e 100 caracteres")
    @Schema(description = "Novo nome completo do usuario autenticado",
            example = "Joao da Silva Atualizado",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String fullName;
}
