package com.nebula.userService.service;

import com.nebula.userService.configs.JwtConfig;
import com.nebula.userService.dto.JwtResponse;
import com.nebula.userService.dto.LoginRequest;
import com.nebula.userService.entities.UserEntity;
import com.nebula.userService.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtConfig jwtConfig;
    private final UserRepository userRepository;
    private final TokenBlacklistService tokenBlacklistService;
    private final AuditLogService auditLogService;

    /** Autentica o usuário e retorna access + refresh token. */
    @Transactional
    public JwtResponse authenticateUser(LoginRequest loginRequest, String ipAddress) {
        UserEntity user = userRepository.findByUsername(loginRequest.getUsername())
                .orElse(null);

        // Conta bloqueada por tentativas falhas?
        if (user != null && user.isLocked()) {
            auditLogService.log(AuditLogService.LOGIN_FAILED, user,
                    "Conta bloqueada", ipAddress);
            throw new AuthenticationServiceException("Conta temporariamente bloqueada. Tente novamente mais tarde.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            user = userRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            // Reseta tentativas falhas e registra last_login_at
            user.registerSuccessfulLogin();
            userRepository.save(user);

            String role = user.getRole() != null ? user.getRole().name() : "USER";
            String accessToken  = jwtConfig.generateToken(user.getUsername(), List.of("ROLE_" + role));
            String refreshToken = jwtConfig.generateRefreshToken(user.getUsername());

            auditLogService.log(AuditLogService.LOGIN_SUCCESS, user,
                    "Login bem-sucedido", ipAddress);

            return new JwtResponse(accessToken, refreshToken);

        } catch (BadCredentialsException e) {
            // Incrementa contador de falhas
            if (user != null) {
                user.registerFailedAttempt();
                userRepository.save(user);
                if (user.isLocked()) {
                    auditLogService.log(AuditLogService.ACCOUNT_LOCKED, user,
                            "Conta bloqueada apos 5 tentativas falhas", ipAddress);
                } else {
                    auditLogService.log(AuditLogService.LOGIN_FAILED, user,
                            "Tentativa " + user.getFailedAttempts() + " de 5", ipAddress);
                }
            } else {
                auditLogService.log(AuditLogService.LOGIN_FAILED,
                        "Username nao encontrado: " + loginRequest.getUsername(), ipAddress);
            }
            throw new AuthenticationServiceException("Invalid credentials", e);
        }
    }

    /** Sobrecarga sem IP para manter compatibilidade com testes. */
    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        return authenticateUser(loginRequest, "unknown");
    }

    /**
     * Usa um refresh token válido para emitir um novo access token.
     */
    public JwtResponse refreshToken(String refreshToken) {
        if (tokenBlacklistService.isBlacklisted(refreshToken)) {
            throw new AuthenticationServiceException("Refresh token revogado");
        }

        Claims claims = jwtConfig.extractClaims(refreshToken);

        if (!jwtConfig.isTokenType(refreshToken, "refresh")) {
            throw new AuthenticationServiceException("Token inválido para refresh");
        }

        String username = claims.getSubject();
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String role = user.getRole() != null ? user.getRole().name() : "USER";
        String newAccessToken = jwtConfig.generateToken(username, List.of("ROLE_" + role));

        return new JwtResponse(newAccessToken, refreshToken);
    }

    /**
     * Revoga o access token e o refresh token (logout).
     */
    public void logout(String accessToken, String refreshToken, String ipAddress) {
        revokeToken(accessToken);
        if (refreshToken != null) {
            revokeToken(refreshToken);
        }

        // Tenta registrar auditoria com o usuário do token
        try {
            Claims claims = jwtConfig.extractClaims(accessToken);
            userRepository.findByUsername(claims.getSubject()).ifPresent(user ->
                    auditLogService.log(AuditLogService.LOGOUT, user, "Logout", ipAddress)
            );
        } catch (Exception ignored) { }
    }

    /** Sobrecarga sem IP para manter compatibilidade. */
    public void logout(String accessToken, String refreshToken) {
        logout(accessToken, refreshToken, "unknown");
    }

    private void revokeToken(String token) {
        try {
            Claims claims = jwtConfig.extractClaims(token);
            long ttlMillis = claims.getExpiration().getTime() - new Date().getTime();
            tokenBlacklistService.blacklist(token, ttlMillis);
        } catch (Exception ignored) { }
    }
}