package com.techpulse.wallet.dto.response;

import java.math.BigDecimal;

public record AdminWalletResponse(
        Long walletId,
        Long userId,
        String email,
        BigDecimal balance,
        Long version
) {}