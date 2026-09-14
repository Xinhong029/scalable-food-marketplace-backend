package com.lastcalleats.user.repository;

import com.lastcalleats.user.entity.UserDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA repository for UserDO.
 * Has extra lookup methods for login and registration, plus standard CRUD from JpaRepository.
 */
@Repository
public interface UserRepo extends JpaRepository<UserDO, Long> {

    /**
     * Find user by email, used during login.
     *
     * @param email the email to search
     * @return Optional with the user, or empty if not found
     */
    Optional<UserDO> findByEmail(String email);

    /**
     * Check if a user with this email already exists.
     * Used during registration to avoid duplicate accounts.
     *
     * @param email the email to check
     * @return true if email is taken, false otherwise
     */
    boolean existsByEmail(String email);
}
