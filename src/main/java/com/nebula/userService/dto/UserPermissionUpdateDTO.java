package com.nebula.userService.dto;

import com.nebula.userService.enums.Permission;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Payload para atualizar permissões finas de um usuário.")
public class UserPermissionUpdateDTO {

    @NotNull(message = "Permissões não podem ser nulas")
    @Schema(description = "Conjunto de permissões refinadas associadas ao usuário.",
            example = "[\"USER_VIEW\", \"USER_EDIT\"]",
            allowableValues = {"USER_VIEW", "USER_EDIT", "USER_DELETE", "SESSION_VIEW", "SESSION_REVOKE", "SESSION_REVOKE_ALL", "PERMISSION_MANAGE", "PROFILE_UPDATE", "PASSWORD_CHANGE"})
    private Set<Permission> permissions;
}
