package com.nebula.userService.dto;

import com.nebula.userService.entities.UserEntity;
import com.nebula.userService.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Payload para criacao de um novo usuario. Este endpoint e publico, mas protegido por rate limiting por IP.")
public class UserRequestDTO {

    @NotBlank(message = "Nome completo é obrigatório")
    @Size(min = 3, max = 100, message = "Nome completo deve ter entre 3 e 100 caracteres")
    @Schema(description = "Nome completo exibido para o usuario",
            example = "Joao da Silva",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String fullName;

    @NotBlank(message = "Username é obrigatório")
    @Size(min = 3, max = 50, message = "Username deve ter entre 3 e 50 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username deve conter apenas letras, números, ponto, hífen ou underscore")
    @Schema(description = "Nome de usuario unico usado no login. Aceita letras, numeros, ponto, hifen e underscore.",
            example = "joao.silva",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ser válido")
    @Schema(description = "Endereco de e-mail unico do usuario",
            example = "joao@empresa.com",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
    @Schema(description = "Senha inicial em texto puro. Minimo atual: 8 caracteres.",
            example = "Senha@123",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @NotNull(message = "Role é obrigatória")
    @Schema(description = "Perfil inicial permitido no cadastro publico. Atualmente, apenas `USER` e aceito neste endpoint.",
            example = "USER",
            allowableValues = {"USER"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Role role;

    public UserEntity toEntity(PasswordEncoder passwordEncoder) {
        return UserEntity.builder()
                .fullName(this.fullName)
                .email(this.email)
                .username(this.username)
                .password(passwordEncoder.encode(this.password))
                .role(this.role)
                .active(true)
                .build();
    }

}
