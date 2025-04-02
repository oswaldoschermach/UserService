package com.VMTecnologia.userService.service;

import com.VMTecnologia.userService.dto.UserRequestDTO;
import com.VMTecnologia.userService.dto.UserResponseDTO;
import com.VMTecnologia.userService.dto.UserUpdateDTO;
import com.VMTecnologia.userService.entities.UserEntity;
import com.VMTecnologia.userService.exception.DatabaseIntegrityException;
import com.VMTecnologia.userService.exception.DuplicateEmailException;
import com.VMTecnologia.userService.exception.DuplicateUsernameException;
import com.VMTecnologia.userService.exception.InvalidRoleException;
import com.VMTecnologia.userService.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;

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

    @Transactional
    public UserResponseDTO createUser(UserRequestDTO userRequestDTO) {
        validateUserRequest(userRequestDTO);

        UserEntity savedUser = userRepository.save(userRequestDTO.toEntity(passwordEncoder));

        sendConfirmationEmail(savedUser);

        return UserResponseDTO.fromEntity(savedUser);
    }

    public Page<UserResponseDTO> findAll(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserResponseDTO::fromEntity);
    }

    public UserResponseDTO updateUser(Long id, UserUpdateDTO userUpdateDTO) {
        validateUserUpdate(id, userUpdateDTO);

        UserEntity existingUser = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.debug("Usuário não encontrado para atualização: ID {}", id);
                    return new EntityNotFoundException("Usuário não encontrado com ID: " + id);
                });

        existingUser.setFullName(userUpdateDTO.getFullName());
        existingUser.setRole(userUpdateDTO.getRole());
        existingUser.setActive(userUpdateDTO.getActive());

        return UserResponseDTO.fromEntity(userRepository.save(existingUser));
    }

    public UserResponseDTO findById(Long id) {
        validateId(id);
        return userRepository.findById(id)
                .map(UserResponseDTO::fromEntity)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    }

    public Page<UserResponseDTO> findByFullName(String fullName, Pageable pageable) {
        validatePagination(fullName, pageable);
        return userRepository.findByFullNameIgnoreCaseContaining(fullName, pageable)
                .map(UserResponseDTO::fromEntity);
    }

    public void deleteUser(Long id) {
        validateId(id);

        if (!userRepository.existsById(id)) {
            throw new EntityNotFoundException("Usuário não encontrado com ID: " + id);
        }

        try {
            userRepository.deleteById(id);
        } catch (DataIntegrityViolationException ex) {
            throw new DataIntegrityViolationException("Não foi possível excluir - usuário possui relacionamentos ativos");
        }
    }

    private void validateUserRequest(UserRequestDTO userRequestDTO) {
        if (userRequestDTO.getRole() == null || userRequestDTO.getRole().isBlank()) {
            throw new IllegalArgumentException("Role é obrigatória");
        }
        if (userRepository.existsByEmail(userRequestDTO.getEmail())) {
            throw new DuplicateEmailException("E-mail já cadastrado: " + userRequestDTO.getEmail());
        }
        if (userRepository.existsByUsername(userRequestDTO.getUsername())) {
            throw new DuplicateUsernameException("Username já cadastrado: " + userRequestDTO.getUsername());
        }
        if (!Arrays.asList("USER", "ADMIN", "MODERATOR").contains(userRequestDTO.getRole().toUpperCase())) {
            throw new InvalidRoleException("Perfil inválido: " + userRequestDTO.getRole());
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

    @TransactionalEventListener
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