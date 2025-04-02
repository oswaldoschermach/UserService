package com.VMTecnologia.userService.exception;

import org.springframework.http.HttpStatus;

public class DatabaseIntegrityException extends BusinessException {
    public DatabaseIntegrityException(String operation) {
        super(
                String.format("Violação de integridade ao %s usuário", operation),
                "DATABASE_INTEGRITY_VIOLATION",
                HttpStatus.CONFLICT
        );
    }
}