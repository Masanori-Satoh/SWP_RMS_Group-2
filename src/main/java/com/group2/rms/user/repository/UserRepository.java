package com.group2.rms.user.repository;

import com.group2.rms.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository cho entity User.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User> {
    Optional<User> findByUsernameIgnoreCase(String username);

    Optional<User> findByEmailIgnoreCase(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.userId = :id")
    Optional<User> findByIdForUpdate(@Param("id") Integer id);

    @Override
    @EntityGraph(attributePaths = {"role", "department"})
    Page<User> findAll(Specification<User> specification, Pageable pageable);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.role LEFT JOIN FETCH u.department")
    java.util.List<User> findAllWithRoleAndDepartment();
}
