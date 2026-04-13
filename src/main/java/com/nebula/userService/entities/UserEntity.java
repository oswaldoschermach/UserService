package com.nebula.userService.entities;

import com.nebula.userService.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Entidade que representa um usuário no sistema. Contém informações de autenticação, perfil e metadados.")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "ID único do usuário (gerado automaticamente)",
            example = "1",
            accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Indica se o usuário está ativo no sistema",
            example = "true",
            defaultValue = "true")
    private Boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Schema(description = "Data e hora de criação do usuário (automática)",
            example = "2023-11-25T10:30:00Z",
            accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @Schema(description = "Data e hora da última atualização do usuário",
            example = "2023-11-25T15:45:00Z",
            accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;

    @Column(name = "full_name", length = 255)
    @Schema(description = "Nome completo do usuário",
            example = "João da Silva",
            minLength = 3,
            maxLength = 255)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Schema(description = "Perfil de acesso do usuário",
            example = "USER",
            allowableValues = {"USER", "ADMIN", "MODERATOR"})
    private Role role;

    @Column(length = 255, unique = true, nullable = false)
    @Schema(description = "Nome de usuário único para login",
            example = "joao.silva",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 3,
            maxLength = 255,
            pattern = "^[a-zA-Z0-9._-]+$")
    private String username;

    @Column(length = 255, unique = true, nullable = false)
    @Schema(description = "Email do usuário (deve ser único)",
            example = "joao@empresa.com",
            requiredMode = Schema.RequiredMode.REQUIRED,
            maxLength = 255,
            pattern = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")
    private String email;

    @Column(length = 255, nullable = false)
    @Schema(description = "Senha criptografada do usuário",
            example = "$2a$10$N9qo8uLOickgx2ZMRZoMy...",
            requiredMode = Schema.RequiredMode.REQUIRED,
            maxLength = 255,
            accessMode = Schema.AccessMode.WRITE_ONLY)
    private String password;

    @Column(name = "last_login_at")
    @Schema(description = "Data e hora do último login bem-sucedido",
            example = "2024-01-15T10:30:00",
            accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime lastLoginAt;

    @Column(name = "failed_attempts", nullable = false)
    @Builder.Default
    @Schema(description = "Número de tentativas de login falhas consecutivas",
            example = "0",
            accessMode = Schema.AccessMode.READ_ONLY)
    private Integer failedAttempts = 0;

    @Column(name = "locked_until")
    @Schema(description = "Conta bloqueada até esta data/hora (nulo = não bloqueada)",
            example = "2024-01-15T10:35:00",
            accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime lockedUntil;

    /** Verifica se a conta está bloqueada no momento. */
    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }

    /** Registra uma tentativa falha e bloqueia após 5 tentativas (15 min). */
    public void registerFailedAttempt() {
        this.failedAttempts = (this.failedAttempts == null ? 0 : this.failedAttempts) + 1;
        if (this.failedAttempts >= 5) {
            this.lockedUntil = LocalDateTime.now().plusMinutes(15);
        }
    }

    /** Reseta o contador de falhas após login bem-sucedido. */
    public void registerSuccessfulLogin() {
        this.failedAttempts = 0;
        this.lockedUntil = null;
        this.lastLoginAt = LocalDateTime.now();
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.active == null) {
            this.active = true;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}