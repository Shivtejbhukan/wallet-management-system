package com.techpulse.wallet.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long transactionId,
        String type,
        String status,
        BigDecimal amount,
        String idempotencyKey,
        LocalDateTime createdAt
) {
}