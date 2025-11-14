package com.zim.paypal.repository;

import com.zim.paypal.model.entity.Account;
import com.zim.paypal.model.entity.Statement;
import com.zim.paypal.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Statement entity
 * 
 * @author Zim Development Team
 */
@Repository
public interface StatementRepository extends JpaRepository<Statement, Long> {

    /**
     * Find statement by statement number
     * 
     * @param statementNumber Statement number
     * @return Optional Statement
     */
    Optional<Statement> findByStatementNumber(String statementNumber);

    /**
     * Find all statements by user
     * 
     * @param user User entity
     * @return List of statements
     */
    List<Statement> findByUserOrderByEndDateDesc(User user);

    /**
     * Find all statements by account
     * 
     * @param account Account entity
     * @return List of statements
     */
    List<Statement> findByAccountOrderByEndDateDesc(Account account);

    /**
     * Find statement by account and date range
     * 
     * @param account Account entity
     * @param startDate Start date
     * @param endDate End date
     * @return Optional Statement
     */
    @Query("SELECT s FROM Statement s WHERE s.account = :account " +
           "AND s.startDate = :startDate AND s.endDate = :endDate")
    Optional<Statement> findByAccountAndDateRange(@Param("account") Account account,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);
}

