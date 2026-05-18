package com.nebula.userService.service;

import com.nebula.userService.dto.CurrentUserUpdateDTO;
import com.nebula.userService.dto.PasswordChangeDTO;
import com.nebula.userService.dto.UserPermissionUpdateDTO;
import com.nebula.userService.dto.UserRequestDTO;
import com.nebula.userService.dto.UserResponseDTO;
import com.nebula.userService.dto.UserUpdateDTO;
import com.nebula.userService.entities.UserEntity;
import com.nebula.userService.enums.Permission;
import com.nebula.userService.enums.Role;
import com.nebula.userService.exception.DatabaseIntegrityException;
import com.nebula.userService.exception.DuplicateEmailException;
import com.nebula.userService.exception.DuplicateUsernameException;
import com.nebula.userService.exception.UserNotFoundException;
import com.nebula.userService.repository.UserRepository;
import com.nebula.userService.service.WebhookService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço para gerenciar operações relacionadas a usuários.
 * <p>
 * Este serviço encapsula a lógica de negócios relacionada a usuários,
 * incluindo operações de CRUD e consultas específicas.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditLogService auditLogService;
    private final WebhookService webhookService;

    @Transactional
    public UserResponseDTO createUser(UserRequestDTO userRequestDTO) {
        validateUserRequest(userRequestDTO);
        UserEntity user = userRequestDTO.toEntity(passwordEncoder);
        user.setPermissions(resolveDefaultPermissions(user.getRole()));
        UserEntity savedUser = userRepository.save(user);
        sendConfirmationEmail(savedUser);
        auditLogService.log(AuditLogService.USER_CREATED, savedUser,
                "User", savedUser.getId(), "Usuario criado: " + savedUser.getUsername(), null);
        webhookService.publishEvent("USER_CREATED", new com.nebula.userService.service.WebhookService.EventPayload(
                "USER_CREATED", null, savedUser.getUsername(), null, null, null, "Usuário criado"));
        return UserResponseDTO.fromEntity(savedUser);
    }

    public Page<UserResponseDTO> findAll(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserResponseDTO::fromEntity);
    }

    @Transactional
    public UserResponseDTO updateUser(Long id, UserUpdateDTO userUpdateDTO) {
        validateUserUpdate(id, userUpdateDTO);

        UserEntity existingUser = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.debug("Usuário não encontrado para atualização: ID {}", id);
                    return new UserNotFoundException(id);
                });

        existingUser.setFullName(userUpdateDTO.getFullName());
        existingUser.setRole(userUpdateDTO.getRole());
        existingUser.setActive(userUpdateDTO.getActive());
        if (existingUser.getPermissions() == null) {
            existingUser.setPermissions(resolveDefaultPermissions(existingUser.getRole()));
        }

        UserEntity saved = userRepository.save(existingUser);
        auditLogService.log(AuditLogService.USER_UPDATED, saved,
                "User", saved.getId(), "Usuario atualizado: " + saved.getUsername(), null);
        webhookService.publishEvent("USER_UPDATED", new com.nebula.userService.service.WebhookService.EventPayload(
                "USER_UPDATED", null, saved.getUsername(), null, null, null, "Usuário atualizado"));
        return UserResponseDTO.fromEntity(saved);
    }

    @Transactional
    public UserResponseDTO updatePermissions(Long id, UserPermissionUpdateDTO dto) {
        UserEntity existingUser = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        existingUser.setPermissions(dto.getPermissions());
        UserEntity saved = userRepository.save(existingUser);
        auditLogService.log(AuditLogService.USER_UPDATED, saved,
                "User", saved.getId(), "Permissões atualizadas: " + dto.getPermissions(), null);
        return UserResponseDTO.fromEntity(saved);
    }

    public UserResponseDTO findCurrentUser(String username) {
        return UserResponseDTO.fromEntity(findUserByUsername(username));
    }

    @Transactional
    public UserResponseDTO updateCurrentUser(String username, CurrentUserUpdateDTO dto) {
        validateCurrentUserUpdate(dto);

        UserEntity existingUser = findUserByUsername(username);
        existingUser.setFullName(dto.getFullName());

        UserEntity saved = userRepository.save(existingUser);
        auditLogService.log(AuditLogService.USER_UPDATED, saved,
                "User", saved.getId(), "Usuario atualizou o proprio perfil: " + saved.getUsername(), null);
        return UserResponseDTO.fromEntity(saved);
    }

    @Transactional
    public void changePassword(String username, PasswordChangeDTO dto, String ipAddress) {
        validatePasswordChange(dto);

        UserEntity user = findUserByUsername(username);

        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new AuthenticationServiceException("Senha atual inválida");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        auditLogService.log(AuditLogService.PASSWORD_CHANGED, user,
                "Troca de senha realizada pelo proprio usuario", ipAddress);
        webhookService.publishEvent("PASSWORD_CHANGED", new com.nebula.userService.service.WebhookService.EventPayload(
                "PASSWORD_CHANGED", null, user.getUsername(), ipAddress, null, null, "Senha alterada pelo usuário"));
    }

    public UserResponseDTO findById(Long id) {
        validateId(id);
        return userRepository.findById(id)
                .map(UserResponseDTO::fromEntity)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    public Page<UserResponseDTO> findByFullName(String fullName, Pageable pageable) {
        validatePagination(fullName, pageable);
        return userRepository.findByFullNameIgnoreCaseContaining(fullName, pageable)
                .map(UserResponseDTO::fromEntity);
    }

    @Transactional
    public void deleteUser(Long id) {
        validateId(id);

        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }

        try {
            userRepository.deleteById(id);
            auditLogService.log(AuditLogService.USER_DELETED, null,
                    "User", id, "Usuario deletado ID: " + id, null);
            webhookService.publishEvent("USER_DELETED", new com.nebula.userService.service.WebhookService.EventPayload(
                    "USER_DELETED", null, null, null, null, null, "Usuário deletado ID: " + id));
        } catch (DataIntegrityViolationException ex) {
            throw new DatabaseIntegrityException("excluir");
        }
    }

    private void validateUserRequest(UserRequestDTO userRequestDTO) {
        if (userRequestDTO.getRole() == null) {
            throw new IllegalArgumentException("Role é obrigatória");
        }
        if (userRequestDTO.getRole() != Role.USER) {
            throw new IllegalArgumentException("Cadastro público permite apenas usuários com role USER");
        }
        if (userRepository.existsByEmail(userRequestDTO.getEmail())) {
            throw new DuplicateEmailException(userRequestDTO.getEmail());
        }
        if (userRepository.existsByUsername(userRequestDTO.getUsername())) {
            throw new DuplicateUsernameException(userRequestDTO.getUsername());
        }
    }

    private void validateUserUpdate(Long id, UserUpdateDTO userUpdateDTO) {
        validateId(id);
        if (userUpdateDTO.getFullName() == null || userUpdateDTO.getFullName().isBlank()) {
            throw new IllegalArgumentException("Nome completo é obrigatório");
        }
    }

    private void validateCurrentUserUpdate(CurrentUserUpdateDTO dto) {
        if (dto.getFullName() == null || dto.getFullName().isBlank()) {
            throw new IllegalArgumentException("Nome completo é obrigatório");
        }
    }

    private void validatePasswordChange(PasswordChangeDTO dto) {
        if (dto.getCurrentPassword() == null || dto.getCurrentPassword().isBlank()) {
            throw new IllegalArgumentException("Senha atual é obrigatória");
        }
        if (dto.getNewPassword() == null || dto.getNewPassword().isBlank()) {
            throw new IllegalArgumentException("Nova senha é obrigatória");
        }
        if (dto.getNewPassword().length() < 8) {
            throw new IllegalArgumentException("A nova senha deve ter no mínimo 8 caracteres");
        }
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID inválido: deve ser um número positivo");
        }
    }

    private void validatePagination(String fullName, Pageable pageable) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("O nome completo não pode ser nulo ou vazio.");
        }
        if (pageable.getPageNumber() < 0 || pageable.getPageSize() <= 0 || pageable.getPageSize() > 100) {
            throw new IllegalArgumentException("Número da página inválido ou tamanho fora do limite permitido (1-100).");
        }
    }

    private java.util.Set<Permission> resolveDefaultPermissions(Role role) {
        if (role == null) {
            return java.util.Collections.emptySet();
        }
        return switch (role) {
            case ADMIN -> java.util.Set.of(Permission.USER_VIEW, Permission.USER_EDIT, Permission.USER_DELETE,
                    Permission.SESSION_VIEW, Permission.SESSION_REVOKE, Permission.SESSION_REVOKE_ALL,
                    Permission.PERMISSION_MANAGE, Permission.PROFILE_UPDATE, Permission.PASSWORD_CHANGE);
            case MODERATOR -> java.util.Set.of(Permission.USER_VIEW, Permission.SESSION_VIEW,
                    Permission.PROFILE_UPDATE, Permission.PASSWORD_CHANGE);
            default -> java.util.Set.of(Permission.PROFILE_UPDATE, Permission.PASSWORD_CHANGE);
        };
    }

    private UserEntity findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Usuario autenticado nao encontrado"));
    }

    public void sendConfirmationEmail(UserEntity user) {
        try {
            emailService.sendEmail(
                    user.getEmail(),
                    "Cadastro realizado com sucesso",
                    "Olá " + user.getFullName() + ",\n\nSeu cadastro foi realizado com sucesso!"
            );
        } catch (EmailService.EmailServiceException ex) {
            log.error("Falha no envio de e-mail de confirmação", ex);
        }
    }
}
