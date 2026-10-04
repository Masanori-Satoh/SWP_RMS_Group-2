package com.group2.rms.user.repository;

import com.group2.rms.user.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Repository cho entity Department.
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, Integer>, JpaSpecificationExecutor<Department> {
    boolean existsByDepartmentNameIgnoreCase(String departmentName);
    boolean existsByDepartmentNameIgnoreCaseAndDepartmentIdNot(String departmentName, Integer departmentId);

    @Override
    @EntityGraph(attributePaths = "manager")
    Page<Department> findAll(Specification<Department> specification, Pageable pageable);
}
