
package com.techpulse.wallet.dto.response;

import java.math.BigDecimal;

public record WalletResponse(
        Long walletId,
        BigDecimal balance,
        Long version
) {}
