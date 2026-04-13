package com.nebula.userService.service;

import com.nebula.userService.entities.PasswordResetTokenEntity;
import com.nebula.userService.entities.UserEntity;
import com.nebula.userService.exception.UserNotFoundException;
import com.nebula.userService.repository.PasswordResetTokenRepository;
import com.nebula.userService.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Gerencia o fluxo de recuperação de senha.
 *
 * Fluxo:
 * 1. requestReset(email)     → gera token, envia e-mail
 * 2. resetPassword(token, newPassword) → valida token, atualiza senha
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final long TOKEN_EXPIRATION_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    /**
     * Solicita a recuperação de senha.
     * Gera um token único, invalida tokens anteriores e envia e-mail.
     * Retorna silenciosamente se o e-mail não existir (evita user enumeration).
     */
    @Transactional
    public void requestReset(String email, String ipAddress) {
        userRepository.findByEmail(email).ifPresent(user -> {
            // Invalida tokens anteriores ainda não usados
            tokenRepository.invalidateAllByUserId(user.getId());

            String rawToken = UUID.randomUUID().toString();

            PasswordResetTokenEntity resetToken = PasswordResetTokenEntity.builder()
                    .user(user)
                    .token(rawToken)
                    .expiresAt(LocalDateTime.now().plusMinutes(TOKEN_EXPIRATION_MINUTES))
                    .used(false)
                    .build();

            tokenRepository.save(resetToken);

            sendResetEmail(user, rawToken);
            auditLogService.log(AuditLogService.PASSWORD_RESET_REQUESTED, user,
                    "Solicitacao de reset de senha", ipAddress);

            log.info("Token de reset gerado para usuario ID {}", user.getId());
        });
    }

    /**
     * Redefine a senha usando um token válido.
     *
     * @param token       token recebido por e-mail
     * @param newPassword nova senha em texto claro (será criptografada)
     */
    @Transactional
    public void resetPassword(String token, String newPassword, String ipAddress) {
        PasswordResetTokenEntity resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido ou expirado"));

        if (!resetToken.isValid()) {
            throw new IllegalArgumentException("Token inválido ou expirado");
        }

        UserEntity user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        // Reseta bloqueio de conta ao redefinir senha
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        auditLogService.log(AuditLogService.PASSWORD_RESET_COMPLETED, user,
                "Senha redefinida com sucesso", ipAddress);

        log.info("Senha redefinida para usuario ID {}", user.getId());
    }

    /** Remove tokens expirados do banco (chamado pelo scheduler). */
    @Transactional
    public void cleanupExpiredTokens() {
        tokenRepository.deleteExpiredTokens(LocalDateTime.now());
        log.debug("Tokens de reset expirados removidos");
    }

    private void sendResetEmail(UserEntity user, String token) {
        String body = String.format(
                "Ola %s,%n%nVoce solicitou a recuperacao de senha.%n%n" +
                "Use o token abaixo (valido por %d minutos):%n%n" +
                "Token: %s%n%n" +
                "Se nao foi voce, ignore este e-mail.",
                user.getFullName(), TOKEN_EXPIRATION_MINUTES, token
        );
        try {
            emailService.sendEmail(user.getEmail(), "Recuperacao de senha", body);
        } catch (EmailService.EmailServiceException ex) {
            log.error("Falha ao enviar e-mail de reset para usuario ID {}", user.getId(), ex);
        }
    }
}
