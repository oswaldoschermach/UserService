package com.nebula.userService.configs;

import com.nebula.userService.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tarefas agendadas de manutenção do sistema.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTasks {

    private final PasswordResetService passwordResetService;

    /**
     * Remove tokens de reset de senha expirados.
     * Executa todo dia à meia-noite.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void cleanupExpiredPasswordResetTokens() {
        log.info("Executando limpeza de tokens de reset expirados...");
        passwordResetService.cleanupExpiredTokens();
    }
}
