package com.zim.paypal.repository;

import com.zim.paypal.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for User entity
 * 
 * @author Zim Development Team
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by username
     * 
     * @param username Username
     * @return Optional User
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email
     * 
     * @param email Email address
     * @return Optional User
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if username exists
     * 
     * @param username Username
     * @return true if exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists
     * 
     * @param email Email address
     * @return true if exists
     */
    boolean existsByEmail(String email);

    /**
     * Find user by username or email
     * 
     * @param username Username
     * @param email Email address
     * @return Optional User
     */
    @Query("SELECT u FROM User u WHERE u.username = :username OR u.email = :email")
    Optional<User> findByUsernameOrEmail(@Param("username") String username, @Param("email") String email);
}

