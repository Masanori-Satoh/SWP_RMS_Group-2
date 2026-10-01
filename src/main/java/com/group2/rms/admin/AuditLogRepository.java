package com.group2.rms.admin;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Lấy tất cả log theo entityName + entityId, sắp xếp mới nhất trước.
     */
    List<AuditLog> findByEntityNameAndEntityIdOrderByTimestampDesc(String entityName, String entityId);
}
