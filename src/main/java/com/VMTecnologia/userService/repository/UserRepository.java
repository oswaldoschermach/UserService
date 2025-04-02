package com.VMTecnologia.userService.repository;

import com.VMTecnologia.userService.entities.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório para gerenciar entidades do tipo UserEntity.
 * <p>
 * Este repositório fornece métodos para realizar operações básicas de CRUD
 * (Criar, Ler, Atualizar e Deletar) e consultas específicas relacionadas aos usuários.
 * As consultas personalizadas são realizadas usando JPA.
 * </p>
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    // Busca por nome (case-insensitive e parcial)
    @Query("SELECT u FROM UserEntity u WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%', :fullName, '%'))")
    Page<UserEntity> findByFullNameIgnoreCaseContaining(@Param("fullName") String fullName, Pageable pageable);

    // Verifica existência por email
    boolean existsByEmail(String email);

    // Novo método para verificar existência por username
    boolean existsByUsername(String username);

    // Busca por username (retorna Optional)
    Optional<UserEntity> findByUsername(String username);

}