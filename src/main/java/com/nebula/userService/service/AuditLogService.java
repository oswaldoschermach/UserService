package com.nebula.userService.service;

import com.nebula.userService.entities.AuditLogEntity;
import com.nebula.userService.entities.UserEntity;
import com.nebula.userService.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Serviço de auditoria — registra ações sensíveis de forma assíncrona
 * para não impactar a latência das requisições principais.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    // ─── Constantes de ação ───────────────────────────────────────────────────

    public static final String LOGIN_SUCCESS    = "LOGIN_SUCCESS";
    public static final String LOGIN_FAILED     = "LOGIN_FAILED";
    public static final String LOGOUT           = "LOGOUT";
    public static final String USER_CREATED     = "USER_CREATED";
    public static final String USER_UPDATED     = "USER_UPDATED";
    public static final String USER_DELETED     = "USER_DELETED";
    public static final String PASSWORD_RESET_REQUESTED = "PASSWORD_RESET_REQUESTED";
    public static final String PASSWORD_RESET_COMPLETED = "PASSWORD_RESET_COMPLETED";
    public static final String ACCOUNT_LOCKED   = "ACCOUNT_LOCKED";

    // ─── Métodos de registro ──────────────────────────────────────────────────

    @Async
    public void log(String action, UserEntity actor, String entity, Long entityId,
                    String detail, String ipAddress) {
        try {
            AuditLogEntity entry = AuditLogEntity.builder()
                    .action(action)
                    .user(actor)
                    .entity(entity)
                    .entityId(entityId)
                    .detail(detail)
                    .ipAddress(ipAddress)
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            // Auditoria nunca deve quebrar o fluxo principal
            log.error("Falha ao registrar audit log. action={}, error={}", action, e.getMessage());
        }
    }

    /** Atalho para ações sem entidade específica (ex: login, logout). */
    @Async
    public void log(String action, UserEntity actor, String detail, String ipAddress) {
        log(action, actor, null, null, detail, ipAddress);
    }

    /** Atalho para ações de sistema sem usuário (ex: tentativa com username inexistente). */
    @Async
    public void log(String action, String detail, String ipAddress) {
        log(action, null, null, null, detail, ipAddress);
    }
}
