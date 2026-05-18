package com.nebula.userService.repository;

import com.nebula.userService.entities.UserSessionEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSessionEntity, Long> {

    List<UserSessionEntity> findByUserUsername(String username);

    Optional<UserSessionEntity> findBySessionId(String sessionId);

    @Modifying
    @Transactional
    @Query("UPDATE UserSessionEntity s SET s.revoked = TRUE WHERE s.user.id = :userId AND s.revoked = FALSE")
    int revokeAllByUserId(@Param("userId") Long userId);
}
