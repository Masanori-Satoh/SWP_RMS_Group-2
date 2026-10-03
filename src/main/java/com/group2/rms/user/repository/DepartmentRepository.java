package com.group2.rms.user.repository;

import com.group2.rms.user.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository cho entity Department.
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, Integer> {
}
