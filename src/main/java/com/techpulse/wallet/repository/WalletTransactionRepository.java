package com.techpulse.wallet.repository;

import com.techpulse.wallet.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface WalletTransactionRepository
        extends JpaRepository<WalletTransaction, Long> {

    Optional<WalletTransaction> findByIdempotencyKey(String idempotencyKey);

    Page<WalletTransaction> findByFromWalletUserEmailOrToWalletUserEmail(
            String fromEmail,
            String toEmail,
            Pageable pageable
    );
}