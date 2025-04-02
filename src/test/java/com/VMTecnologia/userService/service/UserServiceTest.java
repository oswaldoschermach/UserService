package com.VMTecnologia.userService.service;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.VMTecnologia.userService.dto.UserRequestDTO;
import com.VMTecnologia.userService.dto.UserResponseDTO;
import com.VMTecnologia.userService.dto.UserUpdateDTO;
import com.VMTecnologia.userService.entities.UserEntity;
import com.VMTecnologia.userService.exception.DuplicateEmailException;
import com.VMTecnologia.userService.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class UserServiceTest {

    @Autowired
    private UserService userService; // Injeção do UserService

    @MockBean
    private UserRepository userRepository; // Mock do UserRepository

    @MockBean
    private PasswordEncoder passwordEncoder; // Mock do PasswordEncoder

    @MockBean
    private EmailService emailService; // Mock do EmailService

    private UserRequestDTO validUserRequest;

    @BeforeEach
    void setUp() {
        validUserRequest = new UserRequestDTO(
                "João Silva", "joao.silva", "joao@email.com", "Senha123@", "USER");
    }

    @Test
    @DisplayName("createUser - Deve criar usuário com dados válidos")
    @Sql(scripts = "classpath:cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:data.sql")
    void createUser_WithValidData_ShouldCreateUser() {
        // Configuração específica para este teste
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(8L); // Simula um ID gerado para o novo usuário
            return user;
        });

        UserResponseDTO result = userService.createUser(validUserRequest);

        assertNotNull(result);
        assertEquals("João Silva", result.getFullName());
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("createUser - Deve lançar exceção quando email já existe")
    @Sql(scripts = "classpath:cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:data.sql")
    void createUser_WithDuplicateEmail_ShouldThrowException() {
        when(userRepository.existsByEmail(validUserRequest.getEmail())).thenReturn(true);

        assertThrows(DuplicateEmailException.class,
                () -> userService.createUser(validUserRequest));
    }

    @Test
    @DisplayName("updateUser - Deve atualizar usuário existente")
    @Sql(scripts = "classpath:cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:data.sql")
    void updateUser_WithValidData_ShouldUpdateUser() {
        UserUpdateDTO sampleUpdateDTO = new UserUpdateDTO("João Silva Updated", "USER", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(new UserEntity()));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponseDTO result = userService.updateUser(1L, sampleUpdateDTO);

        assertEquals("João Silva Updated", result.getFullName());
    }

    @Test
    @DisplayName("deleteUser - Deve lançar exceção quando usuário possui vínculos")
    @Sql(scripts = "classpath:cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "classpath:data.sql")
    void deleteUser_ShouldThrowWhenUserHasRelations() {
        when(userRepository.existsById(1L)).thenReturn(true);
        doThrow(DataIntegrityViolationException.class).when(userRepository).deleteById(1L);

        assertThrows(DataIntegrityViolationException.class,
                () -> userService.deleteUser(1L));
    }

    @Test
    @DisplayName("findByFullName - Deve rejeitar paginação inválida")
    void findByFullName_ShouldRejectInvalidPagination() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> userService.findByFullName("João", PageRequest.of(-1, 10))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> userService.findByFullName("João", PageRequest.of(0, 0)))
        );
    }
}