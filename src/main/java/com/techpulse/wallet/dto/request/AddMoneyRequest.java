
package com.techpulse.wallet.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AddMoneyRequest(

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01",
                message = "Amount must be greater than zero")
        @Digits(integer = 17, fraction = 2,
                message = "Amount must have at most two decimal places")
        BigDecimal amount

) {}
