package com.wultra.wallet.dto;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Schema(description = "Deposit or withdrawal of money in a single wallet")
public class ActionRequest {

    @Schema(description = "Id of the wallet to deposit to / withdraw from",
            example = "fcb58bd7-f09a-47f2-a92d-89e20095a4da")
    @NotNull
    private UUID walletId;

    @Schema(description = "ISO 4217 currency code. Must match the wallet currency – money is never converted "
            + "on deposit/withdraw, a different currency is rejected with CURRENCY_MISMATCH.",
            implementation = String.class, example = "EUR")
    @NotNull
    private Currency currency;

    @Schema(description = "Amount in the wallet currency, positive, max 2 decimal places", example = "10.50")
    @NotNull
    @Positive
    @Digits(integer = 19, fraction = 2)
    private BigDecimal amount;

}
