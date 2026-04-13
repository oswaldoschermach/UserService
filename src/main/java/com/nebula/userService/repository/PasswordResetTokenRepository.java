package com.nebula.userService.repository;

import com.nebula.userService.entities.PasswordResetTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, Long> {

    Optional<PasswordResetTokenEntity> findByToken(String token);

    /** Invalida todos os tokens anteriores do usuário antes de emitir um novo. */
    @Modifying
    @Query("UPDATE PasswordResetTokenEntity t SET t.used = true WHERE t.user.id = :userId AND t.used = false")
    void invalidateAllByUserId(@Param("userId") Long userId);

    /** Remove tokens expirados (executar periodicamente via @Scheduled). */
    @Modifying
    @Query("DELETE FROM PasswordResetTokenEntity t WHERE t.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
}
