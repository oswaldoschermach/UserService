package com.nebula.userService.service;

import com.nebula.userService.dto.UserRequestDTO;
import com.nebula.userService.dto.UserResponseDTO;
import com.nebula.userService.dto.UserUpdateDTO;
import com.nebula.userService.entities.UserEntity;
import com.nebula.userService.enums.Role;
import com.nebula.userService.exception.DatabaseIntegrityException;
import com.nebula.userService.exception.DuplicateEmailException;
import com.nebula.userService.exception.DuplicateUsernameException;
import com.nebula.userService.exception.UserNotFoundException;
import com.nebula.userService.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private UserService userService;

    private UserEntity userEntity;
    private UserRequestDTO userRequestDTO;
    private UserUpdateDTO userUpdateDTO;

    @BeforeEach
    void setUp() {
        userEntity = UserEntity.builder()
                .id(1L)
                .fullName("Joao Silva")
                .username("joao.silva")
                .email("joao@empresa.com")
                .password("$2a$10$encoded")
                .role(Role.USER)
                .active(true)
                .build();
        userRequestDTO = new UserRequestDTO("Joao Silva", "joao.silva", "joao@empresa.com", "Senha@123", Role.USER);
        userUpdateDTO = new UserUpdateDTO("Joao Silva Atualizado", Role.ADMIN, true);
    }

    @Test
    @DisplayName("createUser - sucesso")
    void createUser_Success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);
        doNothing().when(emailService).sendEmail(anyString(), anyString(), anyString());

        UserResponseDTO result = userService.createUser(userRequestDTO);

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("joao.silva");
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("createUser - email duplicado lanca DuplicateEmailException")
    void createUser_DuplicateEmail_ThrowsException() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(userRequestDTO))
                .isInstanceOf(DuplicateEmailException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("createUser - username duplicado lanca DuplicateUsernameException")
    void createUser_DuplicateUsername_ThrowsException() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(userRequestDTO))
                .isInstanceOf(DuplicateUsernameException.class);
        verify(userRepository, never()).save(any());
    }


    @Test
    @DisplayName("createUser - role nula lanca IllegalArgumentException")
    void createUser_NullRole_ThrowsException() {
        userRequestDTO.setRole(null);

        assertThatThrownBy(() -> userService.createUser(userRequestDTO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("createUser - role ADMIN com sucesso")
    void createUser_AdminRole_Success() {
        userRequestDTO.setRole(Role.ADMIN);
        UserEntity adminEntity = UserEntity.builder()
                .id(2L).fullName("Admin User").username("joao.silva")
                .email("joao@empresa.com").password("encoded")
                .role(Role.ADMIN).active(true).build();
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(UserEntity.class))).thenReturn(adminEntity);

        UserResponseDTO result = userService.createUser(userRequestDTO);

        assertThat(result.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("createUser - role MODERATOR com sucesso")
    void createUser_ModeratorRole_Success() {
        userRequestDTO.setRole(Role.MODERATOR);
        UserEntity modEntity = UserEntity.builder()
                .id(3L).fullName("Mod User").username("joao.silva")
                .email("joao@empresa.com").password("encoded")
                .role(Role.MODERATOR).active(true).build();
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(UserEntity.class))).thenReturn(modEntity);

        UserResponseDTO result = userService.createUser(userRequestDTO);

        assertThat(result.getRole()).isEqualTo(Role.MODERATOR);
    }

    @Test
    @DisplayName("createUser - falha no email nao propaga excecao")
    void createUser_EmailFails_DoesNotPropagate() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);
        doThrow(new EmailService.EmailServiceException("erro email", new RuntimeException()))
                .when(emailService).sendEmail(anyString(), anyString(), anyString());

        UserResponseDTO result = userService.createUser(userRequestDTO);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("updateUser - sucesso")
    void updateUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(userEntity));
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);

        UserResponseDTO result = userService.updateUser(1L, userUpdateDTO);

        assertThat(result).isNotNull();
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("updateUser - usuario nao encontrado lanca UserNotFoundException")
    void updateUser_UserNotFound_ThrowsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(99L, userUpdateDTO))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("updateUser - ID invalido lanca IllegalArgumentException")
    void updateUser_InvalidId_ThrowsException() {
        assertThatThrownBy(() -> userService.updateUser(0L, userUpdateDTO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("updateUser - nome em branco lanca IllegalArgumentException")
    void updateUser_BlankFullName_ThrowsException() {
        userUpdateDTO.setFullName("  ");

        assertThatThrownBy(() -> userService.updateUser(1L, userUpdateDTO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("updateUser - ID nulo lanca IllegalArgumentException")
    void updateUser_NullId_ThrowsException() {
        assertThatThrownBy(() -> userService.updateUser(null, userUpdateDTO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("findById - sucesso")
    void findById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(userEntity));

        UserResponseDTO result = userService.findById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("findById - nao encontrado lanca UserNotFoundException")
    void findById_NotFound_ThrowsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(99L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("findById - ID zero lanca IllegalArgumentException")
    void findById_InvalidId_Zero_ThrowsException() {
        assertThatThrownBy(() -> userService.findById(0L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("findById - ID negativo lanca IllegalArgumentException")
    void findById_NegativeId_ThrowsException() {
        assertThatThrownBy(() -> userService.findById(-1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("findAll - retorna pagina de usuarios")
    void findAll_ReturnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserEntity> page = new PageImpl<>(List.of(userEntity), pageable, 1);
        when(userRepository.findAll(pageable)).thenReturn(page);

        Page<UserResponseDTO> result = userService.findAll(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("findByFullName - sucesso")
    void findByFullName_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserEntity> page = new PageImpl<>(List.of(userEntity), pageable, 1);
        when(userRepository.findByFullNameIgnoreCaseContaining("Joao", pageable)).thenReturn(page);

        Page<UserResponseDTO> result = userService.findByFullName("Joao", pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("findByFullName - nome vazio lanca IllegalArgumentException")
    void findByFullName_EmptyName_ThrowsException() {
        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> userService.findByFullName("", pageable))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("findByFullName - page size invalido lanca IllegalArgumentException")
    void findByFullName_InvalidPageSize_ThrowsException() {
        Pageable pageable = PageRequest.of(0, 200);

        assertThatThrownBy(() -> userService.findByFullName("Joao", pageable))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("findByFullName - nome nulo lanca IllegalArgumentException")
    void findByFullName_NullName_ThrowsException() {
        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> userService.findByFullName(null, pageable))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("deleteUser - sucesso")
    void deleteUser_Success() {
        when(userRepository.existsById(1L)).thenReturn(true);
        doNothing().when(userRepository).deleteById(1L);

        userService.deleteUser(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteUser - usuario nao encontrado lanca UserNotFoundException")
    void deleteUser_UserNotFound_ThrowsException() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(99L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("deleteUser - ID invalido lanca IllegalArgumentException")
    void deleteUser_InvalidId_ThrowsException() {
        assertThatThrownBy(() -> userService.deleteUser(-5L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("deleteUser - violacao de integridade lanca DatabaseIntegrityException")
    void deleteUser_DataIntegrityViolation_ThrowsException() {
        when(userRepository.existsById(1L)).thenReturn(true);
        doThrow(new DataIntegrityViolationException("constraint")).when(userRepository).deleteById(1L);

        assertThatThrownBy(() -> userService.deleteUser(1L))
                .isInstanceOf(DatabaseIntegrityException.class);
    }

    @Test
    @DisplayName("sendConfirmationEmail - sucesso")
    void sendConfirmationEmail_Success() {
        doNothing().when(emailService).sendEmail(anyString(), anyString(), anyString());

        userService.sendConfirmationEmail(userEntity);

        verify(emailService).sendEmail(eq("joao@empresa.com"), anyString(), anyString());
    }

    @Test
    @DisplayName("sendConfirmationEmail - falha no email nao propaga excecao")
    void sendConfirmationEmail_EmailFails_LogsError() {
        doThrow(new EmailService.EmailServiceException("erro", new RuntimeException()))
                .when(emailService).sendEmail(anyString(), anyString(), anyString());

        userService.sendConfirmationEmail(userEntity);
    }
}