package com.nebula.userService.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nebula.userService.configs.JwtAuthenticationFilter;
import com.nebula.userService.configs.JwtConfig;
import com.nebula.userService.dto.JwtResponse;
import com.nebula.userService.dto.LoginRequest;
import com.nebula.userService.service.AuthService;
import com.nebula.userService.service.PasswordResetService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthenticationFilter.class}
        )
)
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @MockitoBean
    private JwtConfig jwtConfig;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser
    @DisplayName("POST login - retorna 200 com token JWT")
    void login_ValidCredentials_Returns200() throws Exception {
        LoginRequest req = new LoginRequest("joao.silva", "Senha@123");
        when(authService.authenticateUser(any(), any())).thenReturn(new JwtResponse("mocked.jwt.token"));

        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mocked.jwt.token"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST login - credenciais invalidas retorna 401")
    void login_InvalidCredentials_Returns401() throws Exception {
        LoginRequest req = new LoginRequest("joao.silva", "errada");
        when(authService.authenticateUser(any(), any()))
                .thenThrow(new AuthenticationServiceException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("POST login - username em branco retorna 400")
    void login_BlankUsername_Returns400() throws Exception {
        LoginRequest req = new LoginRequest("", "Senha@123");

        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("POST login - senha em branco retorna 400")
    void login_BlankPassword_Returns400() throws Exception {
        LoginRequest req = new LoginRequest("joao.silva", "");

        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}