package com.nebula.userService.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends BusinessException {

    public UserNotFoundException(Long id) {
        super(
                String.format("Usuário não encontrado com ID: %d", id),
                "USER_NOT_FOUND",
                HttpStatus.NOT_FOUND
        );
    }

    public UserNotFoundException(String username) {
        super(
                String.format("Usuário não encontrado: %s", username),
                "USER_NOT_FOUND",
                HttpStatus.NOT_FOUND
        );
    }
}
