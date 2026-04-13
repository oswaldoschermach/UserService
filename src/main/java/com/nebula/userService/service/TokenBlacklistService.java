package com.nebula.userService.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Gerencia a blacklist de tokens JWT via Redis.
 * Tokens adicionados à blacklist são considerados inválidos mesmo que ainda não tenham expirado.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "blacklist:token:";

    private final StringRedisTemplate redisTemplate;

    /**
     * Adiciona um token à blacklist pelo tempo restante até expiração.
     *
     * @param token         o token JWT a ser invalidado
     * @param ttlMillis     tempo de vida restante do token em milissegundos
     */
    public void blacklist(String token, long ttlMillis) {
        if (ttlMillis <= 0) {
            return;
        }
        String key = BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(key, "revoked", Duration.ofMillis(ttlMillis));
        log.debug("Token adicionado à blacklist. TTL: {}ms", ttlMillis);
    }

    /**
     * Verifica se um token está na blacklist.
     *
     * @param token o token JWT a verificar
     * @return true se o token foi revogado
     */
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }
}
