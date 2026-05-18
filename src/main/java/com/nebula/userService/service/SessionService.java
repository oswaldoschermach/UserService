package com.nebula.userService.service;

import com.nebula.userService.configs.JwtConfig;
import com.nebula.userService.dto.UserSessionDTO;
import com.nebula.userService.entities.UserEntity;
import com.nebula.userService.entities.UserSessionEntity;
import com.nebula.userService.repository.UserSessionRepository;
import com.nebula.userService.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final UserSessionRepository userSessionRepository;
    private final UserRepository userRepository;
    private final JwtConfig jwtConfig;
    private final WebhookService webhookService;
    private final AuditLogService auditLogService;

    public UserSessionEntity createSession(UserEntity user, String ipAddress, String userAgent) {
        String sessionId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        long expirationMillis = jwtConfig.getExpirationTime();
        LocalDateTime expiresAt = now.plusSeconds(expirationMillis / 1000)
            .plusNanos((int) ((expirationMillis % 1000) * 1_000_000));

        UserSessionEntity session = UserSessionEntity.builder()
                .sessionId(sessionId)
                .user(user)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .createdAt(now)
                .lastSeenAt(now)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();

        UserSessionEntity saved = userSessionRepository.save(session);
        webhookService.publishEvent("SESSION_CREATED", createPayload(user.getUsername(), sessionId, ipAddress, userAgent));
        return saved;
    }

    public List<UserSessionDTO> listSessions(String username) {
        return userSessionRepository.findByUserUsername(username).stream()
                .map(UserSessionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void revokeSession(String sessionId, String username, String ipAddress) {
        UserSessionEntity session = userSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Sessão não encontrada"));
        if (!session.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("Não é possível revogar sessão de outro usuário");
        }
        if (!session.isRevoked()) {
            session.setRevoked(true);
            userSessionRepository.save(session);
            auditLogService.log(AuditLogService.LOGOUT, session.getUser(), "Session", null,
                    "Sessão revogada: " + sessionId, ipAddress);
            webhookService.publishEvent("SESSION_REVOKED", createPayload(username, sessionId, ipAddress, session.getUserAgent()));
        }
    }

    @Transactional
    public void revokeAllSessions(String username, String ipAddress) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        int revokedCount = userSessionRepository.revokeAllByUserId(user.getId());
        auditLogService.log(AuditLogService.LOGOUT, user, "Session", null,
                "Todas as sessões revogadas: " + revokedCount, ipAddress);
        webhookService.publishEvent("ALL_SESSIONS_REVOKED", createPayload(username, null, ipAddress, null));
    }

    public boolean isSessionActive(String sessionId, String username) {
        Optional<UserSessionEntity> session = userSessionRepository.findBySessionId(sessionId);
        return session.filter(UserSessionEntity::isActive)
                .filter(s -> s.getUser().getUsername().equals(username))
                .isPresent();
    }

    @Transactional
    public void refreshSession(String sessionId) {
        userSessionRepository.findBySessionId(sessionId).ifPresent(session -> {
            session.setLastSeenAt(LocalDateTime.now());
            userSessionRepository.save(session);
        });
    }

    private WebhookService.EventPayload createPayload(String username, String sessionId, String ipAddress, String userAgent) {
        WebhookService.EventPayload payload = new WebhookService.EventPayload();
        payload.setUsername(username);
        payload.setSessionId(sessionId);
        payload.setIpAddress(ipAddress);
        payload.setUserAgent(userAgent);
        return payload;
    }
}
