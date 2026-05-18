package com.nebula.userService.dto;

import com.nebula.userService.entities.UserSessionEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Dados de uma sessão ativa de usuário.")
public class UserSessionDTO {

    @Schema(description = "Identificador único da sessão", example = "c1d2f8d1-9e4a-4b8f-b0e0-6da629f12345")
    private String sessionId;

    @Schema(description = "Endereço IP usado na criação da sessão", example = "192.168.0.21")
    private String ipAddress;

    @Schema(description = "User agent informado pelo cliente", example = "Mozilla/5.0 (...)" )
    private String userAgent;

    @Schema(description = "Data/hora da criação da sessão")
    private LocalDateTime createdAt;

    @Schema(description = "Última vez que a sessão foi utilizada")
    private LocalDateTime lastSeenAt;

    @Schema(description = "Data/hora de expiração do access token associado à sessão")
    private LocalDateTime expiresAt;

    @Schema(description = "Indica se a sessão foi revogada")
    private boolean revoked;

    public static UserSessionDTO fromEntity(UserSessionEntity entity) {
        return new UserSessionDTO(
                entity.getSessionId(),
                entity.getIpAddress(),
                entity.getUserAgent(),
                entity.getCreatedAt(),
                entity.getLastSeenAt(),
                entity.getExpiresAt(),
                entity.isRevoked()
        );
    }
}
