package com.nebula.userService.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "Resposta padronizada para erros da API")
public class ErrorResponseDTO {

    @Schema(description = "Código HTTP do erro", example = "404")
    private final int status;

    @Schema(description = "Descrição do tipo de erro", example = "Not Found")
    private final String error;

    @Schema(description = "Mensagem descritiva do erro", example = "Usuário não encontrado com ID: 1")
    private final String message;

    @Schema(description = "Caminho da requisição que gerou o erro", example = "/api/users/1")
    private final String path;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Momento em que o erro ocorreu", example = "2024-01-15T10:30:00")
    private final LocalDateTime timestamp;
}
