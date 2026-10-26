package com.group2.rms.user.repository;

import com.group2.rms.user.entity.Department;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Repository cho entity Department.
 */
@Repository
@SuppressWarnings("null")
public interface DepartmentRepository extends JpaRepository<Department, Integer>, JpaSpecificationExecutor<Department> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Department d where d.departmentId = :id")
    Optional<Department> findForUpdate(@Param("id") Integer id);

    boolean existsByDepartmentNameIgnoreCase(String departmentName);
    boolean existsByDepartmentNameIgnoreCaseAndDepartmentIdNot(String departmentName, Integer departmentId);

    @Override
    @EntityGraph(attributePaths = "manager")
    Page<Department> findAll(Specification<Department> specification, Pageable pageable);
}
