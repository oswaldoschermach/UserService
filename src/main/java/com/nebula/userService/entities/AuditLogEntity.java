package com.nebula.userService.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Registro de auditoria de ações no sistema.
 * Grava quem fez o quê, em qual entidade, quando e de onde.
 */
@Entity
@Table(name = "audit_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Usuário que executou a ação (nulo = sistema ou usuário deletado). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    /** Ação executada. Ex: USER_CREATED, USER_UPDATED, LOGIN_SUCCESS, LOGIN_FAILED. */
    @Column(nullable = false, length = 100)
    private String action;

    /** Nome da entidade afetada. Ex: "User". */
    @Column(length = 100)
    private String entity;

    /** ID da entidade afetada. */
    @Column(name = "entity_id")
    private Long entityId;

    /** Detalhe adicional em texto livre (ex: campos alterados). */
    @Column(columnDefinition = "TEXT")
    private String detail;

    /** IP de origem da requisição. */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
