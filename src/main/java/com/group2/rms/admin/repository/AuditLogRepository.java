package com.group2.rms.admin.repository;

import java.util.List;

import com.group2.rms.admin.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    @Override
    @EntityGraph(attributePaths = "user")
    Page<AuditLog> findAll(Specification<AuditLog> specification, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "user")
    Page<AuditLog> findAll(Pageable pageable);

    @Query("select distinct l.user.userId as id, l.user.fullName as name from AuditLog l order by l.user.fullName, l.user.userId")
    List<ActorChoice> findActors();

    @Query("select distinct l.entityName from AuditLog l order by l.entityName")
    List<String> findEntityNames();

    interface ActorChoice {
        Integer getId();
        String getName();
    }

    /**
     * Lấy tất cả log theo entityName + entityId, sắp xếp mới nhất trước.
     */
    List<AuditLog> findByEntityNameAndEntityIdOrderByTimestampDesc(String entityName, String entityId);
}
