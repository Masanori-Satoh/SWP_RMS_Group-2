package com.group2.rms.repository;

import com.group2.rms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository cho entity User.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
}
