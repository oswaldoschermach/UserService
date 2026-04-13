package com.nebula.userService.config;

import com.nebula.userService.configs.JwtAuthenticationFilter;
import com.nebula.userService.configs.JwtConfig;
import com.nebula.userService.service.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtConfig jwtConfig;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private UserDetails userDetails;

    @Mock
    private Claims claims;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Sem header Authorization - continua a chain")
    void doFilter_NoAuthHeader_ContinuesChain() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtConfig, never()).extractClaims(any());
    }

    @Test
    @DisplayName("Header sem Bearer - continua a chain")
    void doFilter_NonBearerHeader_ContinuesChain() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic abc123");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtConfig, never()).extractClaims(any());
    }

    @Test
    @DisplayName("Token valido - autentica e continua chain")
    void doFilter_ValidToken_AuthenticatesAndContinues() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid.jwt.token");
        when(tokenBlacklistService.isBlacklisted("valid.jwt.token")).thenReturn(false);
        when(jwtConfig.extractClaims("valid.jwt.token")).thenReturn(claims);
        when(jwtConfig.isTokenType("valid.jwt.token", "access")).thenReturn(true);
        when(claims.getSubject()).thenReturn("joao.silva");
        when(claims.get("roles", List.class)).thenReturn(List.of("ROLE_USER"));
        when(userDetailsService.loadUserByUsername("joao.silva")).thenReturn(userDetails);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    @DisplayName("Token expirado - retorna 401")
    void doFilter_ExpiredToken_Returns401() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer expired.token");
        when(tokenBlacklistService.isBlacklisted("expired.token")).thenReturn(false);
        when(jwtConfig.extractClaims("expired.token")).thenThrow(mock(ExpiredJwtException.class));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(response).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), anyString());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Token invalido - retorna 401")
    void doFilter_InvalidToken_Returns401() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid.token");
        when(tokenBlacklistService.isBlacklisted("invalid.token")).thenReturn(false);
        when(jwtConfig.extractClaims("invalid.token")).thenThrow(new RuntimeException("Token parse error"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(response).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), anyString());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Token com roles nulas - autentica com sucesso")
    void doFilter_NullRolesInToken_AuthenticatesSuccessfully() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid.jwt.token");
        when(tokenBlacklistService.isBlacklisted("valid.jwt.token")).thenReturn(false);
        when(jwtConfig.extractClaims("valid.jwt.token")).thenReturn(claims);
        when(jwtConfig.isTokenType("valid.jwt.token", "access")).thenReturn(true);
        when(claims.getSubject()).thenReturn("joao.silva");
        when(claims.get("roles", List.class)).thenReturn(null);
        when(userDetailsService.loadUserByUsername("joao.silva")).thenReturn(userDetails);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }
}