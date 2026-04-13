package com.nebula.userService.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nebula.userService.configs.JwtAuthenticationFilter;
import com.nebula.userService.configs.JwtConfig;
import com.nebula.userService.dto.UserRequestDTO;
import com.nebula.userService.dto.UserResponseDTO;
import com.nebula.userService.dto.UserUpdateDTO;
import com.nebula.userService.enums.Role;
import com.nebula.userService.exception.DuplicateEmailException;
import com.nebula.userService.exception.UserNotFoundException;
import com.nebula.userService.service.RateLimitService;
import com.nebula.userService.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = UserController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthenticationFilter.class}
        )
)
@DisplayName("UserController Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private RateLimitService rateLimitService;

    @MockitoBean
    private JwtConfig jwtConfig;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private UserResponseDTO userResponseDTO;
    private UserRequestDTO userRequestDTO;

    @BeforeEach
    void setUp() {
        userResponseDTO = new UserResponseDTO(1L, "Joao Silva", Role.USER, "joao.silva", "joao@empresa.com", true);
        userRequestDTO = new UserRequestDTO("Joao Silva", "joao.silva", "joao@empresa.com", "Senha@123", Role.USER);
        when(rateLimitService.tryConsume(any())).thenReturn(true);
    }

    @Test
    @WithMockUser
    @DisplayName("POST createUser - retorna 201")
    void createUser_Returns201() throws Exception {
        when(userService.createUser(any())).thenReturn(userResponseDTO);

        mockMvc.perform(post("/api/users/createUser").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("joao.silva"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @WithMockUser
    @DisplayName("POST createUser - dados invalidos retorna 400")
    void createUser_InvalidData_Returns400() throws Exception {
        UserRequestDTO invalid = new UserRequestDTO("", "", "emailinvalido", "123", Role.USER);

        mockMvc.perform(post("/api/users/createUser").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("POST createUser - email duplicado retorna 409")
    void createUser_DuplicateEmail_Returns409() throws Exception {
        when(userService.createUser(any())).thenThrow(new DuplicateEmailException("joao@empresa.com"));

        mockMvc.perform(post("/api/users/createUser").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequestDTO)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT updateUser - retorna 200")
    void updateUser_Returns200() throws Exception {
        UserUpdateDTO updateDTO = new UserUpdateDTO("Joao Atualizado", Role.ADMIN, true);
        UserResponseDTO updated = new UserResponseDTO(1L, "Joao Atualizado", Role.ADMIN, "joao.silva", "joao@empresa.com", true);
        when(userService.updateUser(eq(1L), any())).thenReturn(updated);

        mockMvc.perform(put("/api/users/1").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Joao Atualizado"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT updateUser - usuario nao encontrado retorna 404")
    void updateUser_NotFound_Returns404() throws Exception {
        UserUpdateDTO updateDTO = new UserUpdateDTO("Joao Atualizado", Role.ADMIN, true);
        when(userService.updateUser(eq(99L), any())).thenThrow(new UserNotFoundException(99L));

        mockMvc.perform(put("/api/users/99").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT updateUser - nome em branco retorna 400")
    void updateUser_BlankName_Returns400() throws Exception {
        UserUpdateDTO invalid = new UserUpdateDTO("", Role.USER, true);

        mockMvc.perform(put("/api/users/1").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("GET findById - retorna 200")
    void findById_Returns200() throws Exception {
        when(userService.findById(1L)).thenReturn(userResponseDTO);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("joao.silva"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET findById - nao encontrado retorna 404")
    void findById_NotFound_Returns404() throws Exception {
        when(userService.findById(99L)).thenThrow(new UserNotFoundException(99L));

        mockMvc.perform(get("/api/users/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    @DisplayName("GET search - com nome retorna lista paginada")
    void findByFullName_WithName_Returns200() throws Exception {
        Page<UserResponseDTO> page = new PageImpl<>(List.of(userResponseDTO), PageRequest.of(0, 10), 1);
        when(userService.findByFullName(eq("Joao"), any())).thenReturn(page);

        mockMvc.perform(get("/api/users/search")
                        .param("fullName", "Joao")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    @WithMockUser
    @DisplayName("GET search - sem nome chama findAll")
    void findByFullName_NoName_CallsFindAll() throws Exception {
        Page<UserResponseDTO> page = new PageImpl<>(List.of(userResponseDTO), PageRequest.of(0, 10), 1);
        when(userService.findAll(any())).thenReturn(page);

        mockMvc.perform(get("/api/users/search")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());

        verify(userService).findAll(any());
        verify(userService, never()).findByFullName(anyString(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE deleteUser - retorna 204")
    void deleteUser_Returns204() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/api/users/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE deleteUser - nao encontrado retorna 404")
    void deleteUser_NotFound_Returns404() throws Exception {
        doThrow(new UserNotFoundException(99L)).when(userService).deleteUser(99L);

        mockMvc.perform(delete("/api/users/99").with(csrf()))
                .andExpect(status().isNotFound());
    }
}