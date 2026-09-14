package com.lastcalleats.merchant.repository;

import com.lastcalleats.merchant.entity.MerchantDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA repository for MerchantDO.
 * Has extra lookup methods for auth and profile, plus the standard CRUD from JpaRepository.
 */
@Repository
public interface MerchantRepo extends JpaRepository<MerchantDO, Long> {

    /**
     * Find merchant by email, used during login.
     *
     * @param email the email to search
     * @return Optional with the merchant, or empty if not found
     */
    Optional<MerchantDO> findByEmail(String email);

    /**
     * Check if a merchant with this email already exists.
     * Used during registration to avoid duplicate accounts.
     *
     * @param email the email to check
     * @return true if email is taken, false otherwise
     */
    boolean existsByEmail(String email);
}
