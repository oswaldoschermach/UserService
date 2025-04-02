package com.VMTecnologia.userService.dto;

import com.VMTecnologia.userService.entities.UserEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Schema(description = "DTO de resposta com os dados do usuário retornados pela API")
public class UserResponseDTO {

    @Schema(description = "ID único do usuário no sistema",
            example = "123",
            accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Nome completo do usuário",
            example = "João da Silva",
            minLength = 3,
            maxLength = 255)
    private String fullName;

    @Schema(description = "Perfil de acesso do usuário",
            example = "USER",
            allowableValues = {"USER", "ADMIN", "MODERATOR"})
    private String role;

    @Schema(description = "Nome de usuário único para login",
            example = "joao.silva",
            minLength = 3,
            maxLength = 255)
    private String username;

    @Schema(description = "Email cadastrado do usuário",
            example = "joao@empresa.com",
            pattern = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")
    private String email;

    @Schema(description = "Indica se o usuário está ativo no sistema",
            example = "true",
            defaultValue = "true")
    private Boolean active;

    @Schema(description = "Converte a entidade UserEntity para UserResponseDTO",
            hidden = true)
    public static UserResponseDTO fromEntity(UserEntity user) {
        return new UserResponseDTO(
                user.getId(),
                user.getFullName(),
                user.getRole(),
                user.getUsername(),
                user.getEmail(),
                user.getActive()
        );
    }
}