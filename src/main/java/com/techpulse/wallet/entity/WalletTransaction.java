package com.techpulse.wallet.entity;

import com.techpulse.wallet.enums.TransactionStatus;
import com.techpulse.wallet.enums.TransactionType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "wallet_transactions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_transaction_idempotency_key",
                        columnNames = "idempotency_key"
                )
        },
        indexes = {
                @Index(
                        name = "idx_txn_created_at",
                        columnList = "created_at"
                )
        }
)
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Null for ADD transactions; populated for TRANSFER transactions.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_wallet_id")
    private Wallet fromWallet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_wallet_id", nullable = false)
    private Wallet toWallet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(
            name = "idempotency_key",
            nullable = false,
            unique = true,
            length = 100
    )
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Default constructor required by JPA
    public WalletTransaction() {
    }

    // Constructor used by WalletServiceImpl
    public WalletTransaction(
            Wallet fromWallet,
            Wallet toWallet,
            TransactionType type,
            TransactionStatus status,
            BigDecimal amount,
            String idempotencyKey) {

        this.fromWallet = fromWallet;
        this.toWallet = toWallet;
        this.type = type;
        this.status = status;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // Getters

    public Long getId() {
        return id;
    }

    public Wallet getFromWallet() {
        return fromWallet;
    }

    public Wallet getToWallet() {
        return toWallet;
    }

    public TransactionType getType() {
        return type;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Setters

    public void setFromWallet(Wallet fromWallet) {
        this.fromWallet = fromWallet;
    }

    public void setToWallet(Wallet toWallet) {
        this.toWallet = toWallet;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}