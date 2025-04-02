package com.VMTecnologia.userService.exception;

import org.springframework.http.HttpStatus;

public class DuplicateEmailException extends BusinessException {
    public DuplicateEmailException(String email) {
        super(
                String.format("O e-mail '%s' já está cadastrado", email),
                "DUPLICATE_EMAIL",
                HttpStatus.BAD_REQUEST
        );
    }
}