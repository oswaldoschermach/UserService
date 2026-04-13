package com.nebula.userService.repository;

import com.nebula.userService.entities.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

    Page<AuditLogEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<AuditLogEntity> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);
}
