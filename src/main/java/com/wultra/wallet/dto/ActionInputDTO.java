package com.wultra.wallet.dto;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ActionInputDTO {

    @NotNull
    private UUID walletId;

    /// todo description for doc
    /** Must match the wallet currency; money is never converted on deposit/withdraw. */
    @NotNull
    private Currency currency;

    @NotNull
    @Positive
    @Digits(integer = 19, fraction = 4)
    private BigDecimal amount;

}
