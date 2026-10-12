package com.wultra.wallet.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Total balance of all user's wallets converted to one currency")
public class BalanceResponse {

    @Schema(description = "Sum of all wallets converted to the requested currency", example = "12.80")
    private BigDecimal balance;

    @Schema(description = "Currency of the balance (ISO 4217)", implementation = String.class, example = "USD")
    private Currency currency;

    @Schema(description = "Date of the CNB exchange rates used for the conversion (CNB 'validFor'). "
            + "On weekends and public holidays it is the last working day.",
            example = "2026-10-09", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate ratesValidForDate;

}
