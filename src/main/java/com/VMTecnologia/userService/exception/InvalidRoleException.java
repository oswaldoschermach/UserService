package com.VMTecnologia.userService.exception;

import org.springframework.http.HttpStatus;

public class InvalidRoleException extends BusinessException {
    public InvalidRoleException(String role) {
        super(
                String.format("Perfil inválido: '%s' (use USER, ADMIN ou MODERATOR)", role),
                "INVALID_ROLE",
                HttpStatus.BAD_REQUEST
        );
    }
}