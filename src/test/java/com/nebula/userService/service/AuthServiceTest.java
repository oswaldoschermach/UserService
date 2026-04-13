package com.nebula.userService.service;

import com.nebula.userService.configs.JwtConfig;
import com.nebula.userService.dto.JwtResponse;
import com.nebula.userService.dto.LoginRequest;
import com.nebula.userService.entities.UserEntity;
import com.nebula.userService.enums.Role;
import com.nebula.userService.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtConfig jwtConfig;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AuthService authService;

    private LoginRequest loginRequest;
    private UserEntity userEntity;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest("joao.silva", "Senha@123");
        userEntity = UserEntity.builder()
                .id(1L)
                .username("joao.silva")
                .email("joao@empresa.com")
                .password("$2a$encoded")
                .role(Role.USER)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("authenticateUser - sucesso retorna token JWT")
    void authenticateUser_Success() {
        Authentication auth = new UsernamePasswordAuthenticationToken("joao.silva", null);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userRepository.findByUsername("joao.silva")).thenReturn(Optional.of(userEntity));
        when(jwtConfig.generateToken(anyString(), anyList())).thenReturn("mocked.jwt.token");
        when(jwtConfig.generateRefreshToken(anyString())).thenReturn("mocked.refresh.token");

        JwtResponse result = authService.authenticateUser(loginRequest);

        assertThat(result).isNotNull();
        assertThat(result.getToken()).isEqualTo("mocked.jwt.token");
        assertThat(result.getRefreshToken()).isEqualTo("mocked.refresh.token");
        verify(jwtConfig).generateToken(eq("joao.silva"), anyList());
        verify(jwtConfig).generateRefreshToken("joao.silva");
    }

    @Test
    @DisplayName("authenticateUser - gera token com ROLE_USER")
    void authenticateUser_GeneratesCorrectRole() {
        Authentication auth = new UsernamePasswordAuthenticationToken("joao.silva", null);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userRepository.findByUsername("joao.silva")).thenReturn(Optional.of(userEntity));
        when(jwtConfig.generateToken(anyString(), anyList())).thenReturn("mocked.jwt.token");
        when(jwtConfig.generateRefreshToken(anyString())).thenReturn("mocked.refresh.token");

        authService.authenticateUser(loginRequest);

        verify(jwtConfig).generateToken(eq("joao.silva"), argThat(roles ->
                roles.size() == 1 && roles.get(0).equals("ROLE_USER")));
    }

    @Test
    @DisplayName("authenticateUser - role nula usa USER como padrao")
    void authenticateUser_NullRole_DefaultsToUser() {
        userEntity.setRole(null);
        Authentication auth = new UsernamePasswordAuthenticationToken("joao.silva", null);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userRepository.findByUsername("joao.silva")).thenReturn(Optional.of(userEntity));
        when(jwtConfig.generateToken(anyString(), anyList())).thenReturn("mocked.jwt.token");
        when(jwtConfig.generateRefreshToken(anyString())).thenReturn("mocked.refresh.token");

        authService.authenticateUser(loginRequest);

        verify(jwtConfig).generateToken(eq("joao.silva"), argThat(roles ->
                roles.size() == 1 && roles.get(0).equals("ROLE_USER")));
    }

    @Test
    @DisplayName("authenticateUser - role ADMIN gera ROLE_ADMIN no token")
    void authenticateUser_AdminRole_Success() {
        userEntity.setRole(Role.ADMIN);
        Authentication auth = new UsernamePasswordAuthenticationToken("joao.silva", null);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userRepository.findByUsername("joao.silva")).thenReturn(Optional.of(userEntity));
        when(jwtConfig.generateToken(anyString(), anyList())).thenReturn("admin.jwt.token");
        when(jwtConfig.generateRefreshToken(anyString())).thenReturn("admin.refresh.token");

        JwtResponse result = authService.authenticateUser(loginRequest);

        assertThat(result.getToken()).isEqualTo("admin.jwt.token");
        verify(jwtConfig).generateToken(eq("joao.silva"), argThat(roles ->
                roles.get(0).equals("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("authenticateUser - credenciais invalidas lanca AuthenticationServiceException")
    void authenticateUser_BadCredentials_ThrowsException() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.authenticateUser(loginRequest))
                .isInstanceOf(AuthenticationServiceException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    @DisplayName("authenticateUser - usuario nao no repositorio lanca excecao")
    void authenticateUser_UserNotInRepository_ThrowsException() {
        Authentication auth = new UsernamePasswordAuthenticationToken("joao.silva", null);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userRepository.findByUsername("joao.silva")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.authenticateUser(loginRequest))
                .isInstanceOf(Exception.class);
    }
}