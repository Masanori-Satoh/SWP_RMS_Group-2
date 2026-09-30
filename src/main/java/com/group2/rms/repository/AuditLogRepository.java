package com.group2.rms.repository;

import com.group2.rms.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository cho entity AuditLog.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
