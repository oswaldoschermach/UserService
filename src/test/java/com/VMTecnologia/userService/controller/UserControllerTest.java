package com.VMTecnologia.userService.controller;

import com.VMTecnologia.userService.dto.UserResponseDTO;
import com.VMTecnologia.userService.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(UserController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void getUser_shouldReturn200WhenUserExists() throws Exception {
        when(userService.findById(1L))
                .thenReturn(new UserResponseDTO(
                        1L,                 // id
                        "Admin",            // fullName
                        "ADMIN",            // role - CORRIGIDO: estava email
                        "admin_username",   // username
                        "admin@email.com",  // email - CORRIGIDO: estava role
                        true                // active
                ));

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@email.com"))
                .andExpect(jsonPath("$.username").value("admin_username"));
    }

    @Test
    void getUser_shouldReturn404WhenUserNotFound() throws Exception {
        when(userService.findById(999L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound());
    }
}