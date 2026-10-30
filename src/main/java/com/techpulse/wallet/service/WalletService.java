package com.techpulse.wallet.service;

import com.techpulse.wallet.dto.response.TransactionResponse;
import com.techpulse.wallet.dto.response.WalletResponse;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public interface WalletService {
    WalletResponse getMyWallet(String email);

    TransactionResponse addMoney(
            String email,
            java.math.BigDecimal amount,
            String idempotencyKey);

    TransactionResponse transferMoney(
            String email,
            Long receiverUserId,
            BigDecimal amount,
            String idempotencyKey
    );

    Page<TransactionResponse> getMyTransactions(
            String email,
            int page,
            int size
    );

}
