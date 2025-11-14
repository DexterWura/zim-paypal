package com.zim.paypal.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Statement entity representing account statements
 * 
 * @author Zim Development Team
 */
@Entity
@Table(name = "statements", indexes = {
    @Index(name = "idx_statement_user", columnList = "user_id"),
    @Index(name = "idx_statement_account", columnList = "account_id"),
    @Index(name = "idx_statement_period", columnList = "start_date, end_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"user", "account"})
@ToString(exclude = {"user", "account"})
public class Statement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "statement_number", nullable = false, unique = true, length = 30)
    private String statementNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    @NotNull(message = "Account is required")
    private Account account;

    @Column(name = "start_date", nullable = false)
    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @Column(name = "opening_balance", nullable = false, precision = 19, scale = 2)
    @NotNull(message = "Opening balance is required")
    private BigDecimal openingBalance;

    @Column(name = "closing_balance", nullable = false, precision = 19, scale = 2)
    @NotNull(message = "Closing balance is required")
    private BigDecimal closingBalance;

    @Column(name = "total_credits", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalCredits = BigDecimal.ZERO;

    @Column(name = "total_debits", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalDebits = BigDecimal.ZERO;

    @Column(name = "transaction_count")
    @Builder.Default
    private Integer transactionCount = 0;

    @Column(name = "currency_code", nullable = false, length = 3)
    @Builder.Default
    private String currencyCode = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "statement_type", nullable = false)
    @Builder.Default
    private StatementType statementType = StatementType.MONTHLY;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "generated")
    @Builder.Default
    private Boolean generated = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Enumeration for statement types
     */
    public enum StatementType {
        DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM
    }
}

