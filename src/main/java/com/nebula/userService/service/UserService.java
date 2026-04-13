package com.nebula.userService.service;

import com.nebula.userService.dto.UserRequestDTO;
import com.nebula.userService.dto.UserResponseDTO;
import com.nebula.userService.dto.UserUpdateDTO;
import com.nebula.userService.entities.UserEntity;
import com.nebula.userService.exception.DatabaseIntegrityException;
import com.nebula.userService.exception.DuplicateEmailException;
import com.nebula.userService.exception.DuplicateUsernameException;
import com.nebula.userService.exception.UserNotFoundException;
import com.nebula.userService.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Transactional
    public UserResponseDTO createUser(UserRequestDTO userRequestDTO) {
        validateUserRequest(userRequestDTO);
        UserEntity savedUser = userRepository.save(userRequestDTO.toEntity(passwordEncoder));
        sendConfirmationEmail(savedUser);
        auditLogService.log(AuditLogService.USER_CREATED, savedUser,
                "User", savedUser.getId(), "Usuario criado: " + savedUser.getUsername(), null);
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

        UserEntity saved = userRepository.save(existingUser);
        auditLogService.log(AuditLogService.USER_UPDATED, saved,
                "User", saved.getId(), "Usuario atualizado: " + saved.getUsername(), null);
        return UserResponseDTO.fromEntity(saved);
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
        } catch (DataIntegrityViolationException ex) {
            throw new DatabaseIntegrityException("excluir");
        }
    }

    private void validateUserRequest(UserRequestDTO userRequestDTO) {
        if (userRequestDTO.getRole() == null) {
            throw new IllegalArgumentException("Role é obrigatória");
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
