package com.techpulse.wallet.service.Impl;

import com.techpulse.wallet.dto.response.AdminWalletResponse;
import com.techpulse.wallet.dto.response.TransactionResponse;
import com.techpulse.wallet.entity.Wallet;
import com.techpulse.wallet.entity.WalletTransaction;
import com.techpulse.wallet.repository.WalletRepository;
import com.techpulse.wallet.repository.WalletTransactionRepository;
import com.techpulse.wallet.service.AdminService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminServiceImpl implements AdminService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    public AdminServiceImpl(
            WalletRepository walletRepository,
            WalletTransactionRepository walletTransactionRepository) {

        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminWalletResponse> getAllWallets() {

        return walletRepository.findAll()
                .stream()
                .map(wallet -> new AdminWalletResponse(
                        wallet.getId(),
                        wallet.getUser().getId(),
                        wallet.getUser().getEmail(),
                        wallet.getBalance(),
                        wallet.getVersion()
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getAllTransactions(int page, int size) {

        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page number cannot be negative");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and 100");
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<WalletTransaction> transactions =
                walletTransactionRepository.findAll(pageable);

        return transactions.map(transaction -> new TransactionResponse(
                transaction.getId(),
                transaction.getType().name(),
                transaction.getStatus().name(),
                transaction.getAmount(),
                transaction.getIdempotencyKey(),
                transaction.getCreatedAt()
        ));
    }
}