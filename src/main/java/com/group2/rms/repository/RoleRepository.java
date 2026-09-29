package com.group2.rms.repository;

import com.group2.rms.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository cho entity Role.
 * JpaRepository<Role, Integer> cung cấp sẵn: save(), findAll(), count(), deleteById(),...
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByRoleName(String roleName);
}
