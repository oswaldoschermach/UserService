package com.VMTecnologia.userService.exception;
import org.springframework.http.HttpStatus;

public class DuplicateUsernameException extends BusinessException {
    public DuplicateUsernameException(String username) {
        super(
                String.format("O username '%s' já está em uso", username),
                "DUPLICATE_USERNAME",
                HttpStatus.CONFLICT
        );
    }
}