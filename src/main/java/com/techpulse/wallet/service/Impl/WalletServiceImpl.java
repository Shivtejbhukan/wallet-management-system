package com.techpulse.wallet.service.Impl;

import com.techpulse.wallet.dto.response.TransactionResponse;
import com.techpulse.wallet.dto.response.WalletResponse;
import com.techpulse.wallet.entity.User;
import com.techpulse.wallet.entity.Wallet;
import com.techpulse.wallet.entity.WalletTransaction;
import com.techpulse.wallet.enums.TransactionStatus;
import com.techpulse.wallet.enums.TransactionType;
import com.techpulse.wallet.repository.UserRepository;
import com.techpulse.wallet.repository.WalletRepository;
import com.techpulse.wallet.repository.WalletTransactionRepository;
import com.techpulse.wallet.service.WalletService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class WalletServiceImpl implements WalletService {


    private TransactionResponse toTransactionResponse(
            WalletTransaction transaction) {

        return new TransactionResponse(
                transaction.getId(),
                transaction.getType().name(),
                transaction.getStatus().name(),
                transaction.getAmount(),
                transaction.getIdempotencyKey(),
                transaction.getCreatedAt()
        );
    }

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    public WalletServiceImpl(
            UserRepository userRepository,
            WalletRepository walletRepository, WalletTransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public WalletResponse getMyWallet(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Wallet not found"));

        return new WalletResponse(
                wallet.getId(),
                wallet.getBalance(),
                wallet.getVersion()
        );
    }




    @Override
    @Transactional
    public TransactionResponse addMoney(
            String email,
            BigDecimal amount,
            String idempotencyKey) {

        if (amount == null
                || amount.signum() <= 0
                || amount.scale() > 2) {
            throw new IllegalArgumentException(
                    "Amount must be positive with at most two decimal places");
        }

        if (idempotencyKey == null
                || idempotencyKey.isBlank()
                || idempotencyKey.length() > 100) {
            throw new IllegalArgumentException(
                    "A valid Idempotency-Key header is required");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        // Lock this user's wallet before checking the key.
        Wallet wallet = walletRepository.findByUserIdForUpdate(user.getId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Wallet not found"));

        // Check idempotency after acquiring the wallet lock.
        var existing = transactionRepository
                .findByIdempotencyKey(idempotencyKey);

        if (existing.isPresent()) {
            WalletTransaction transaction = existing.get();

            if (!transaction.getToWallet().getUser()
                    .getEmail().equals(email)
                    || transaction.getType() != TransactionType.ADD
                    || transaction.getAmount().compareTo(amount) != 0) {

                throw new IllegalArgumentException(
                        "Idempotency key was already used for a different request");
            }

            return toTransactionResponse(transaction);
        }

        // Add money only when this is a new request.
        wallet.setBalance(wallet.getBalance().add(amount));

        WalletTransaction transaction = new WalletTransaction(
                null,
                wallet,
                TransactionType.ADD,
                TransactionStatus.SUCCESS,
                amount,
                idempotencyKey
        );

        WalletTransaction saved = transactionRepository.save(transaction);

        walletRepository.save(wallet);

        return toTransactionResponse(saved);
    }




    @Override
    @Transactional
    public TransactionResponse transferMoney(
            String email,
            Long receiverUserId,
            BigDecimal amount,
            String idempotencyKey) {

        // 1. Validate amount
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2) {
            throw new IllegalArgumentException(
                    "Amount must be positive with at most two decimal places");
        }

        // 2. Validate idempotency key
        if (idempotencyKey == null
                || idempotencyKey.isBlank()
                || idempotencyKey.length() > 100) {
            throw new IllegalArgumentException(
                    "A valid Idempotency-Key header is required");
        }

        // 3. Find sender
        User sender = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("Sender not found"));

        // 4. Prevent self-transfer
        if (sender.getId().equals(receiverUserId)) {
            throw new IllegalArgumentException(
                    "You cannot transfer money to yourself");
        }

        // 5. Find receiver
        User receiver = userRepository.findById(receiverUserId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Receiver not found"));

        // 6. Lock both wallets in consistent order
        Long firstUserId = Math.min(sender.getId(), receiver.getId());
        Long secondUserId = Math.max(sender.getId(), receiver.getId());

        Wallet firstWallet = walletRepository
                .findByUserIdForUpdate(firstUserId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Wallet not found"));

        Wallet secondWallet = walletRepository
                .findByUserIdForUpdate(secondUserId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Wallet not found"));

        Wallet senderWallet = sender.getId().equals(firstUserId)
                ? firstWallet : secondWallet;

        Wallet receiverWallet = receiver.getId().equals(firstUserId)
                ? firstWallet : secondWallet;

        // 7. Check idempotency AFTER acquiring wallet locks
        var existing = transactionRepository
                .findByIdempotencyKey(idempotencyKey);

        if (existing.isPresent()) {
            WalletTransaction transaction = existing.get();

            if (transaction.getType() != TransactionType.TRANSFER
                    || !transaction.getFromWallet().getUser()
                    .getEmail().equals(email)
                    || !transaction.getToWallet().getUser()
                    .getId().equals(receiverUserId)
                    || transaction.getAmount().compareTo(amount) != 0) {

                throw new IllegalArgumentException(
                        "Idempotency key was already used for a different request");
            }

            return toTransactionResponse(transaction);
        }

        // 8. Check available balance
        if (senderWallet.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException(
                    "Insufficient wallet balance");
        }

        // 9. Update both balances
        senderWallet.setBalance(senderWallet.getBalance().subtract(amount));
        receiverWallet.setBalance(receiverWallet.getBalance().add(amount));

        walletRepository.save(senderWallet);
        walletRepository.save(receiverWallet);

        // 10. Save transaction
        WalletTransaction transaction = new WalletTransaction(
                senderWallet,
                receiverWallet,
                TransactionType.TRANSFER,
                TransactionStatus.SUCCESS,
                amount,
                idempotencyKey
        );

        WalletTransaction saved = transactionRepository.save(transaction);

        return toTransactionResponse(saved);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getMyTransactions(
            String email, int page, int size) {

        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
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
                transactionRepository.findByFromWalletUserEmailOrToWalletUserEmail(
                                email, email, pageable);

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