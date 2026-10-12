package com.wultra.wallet.dto;

import java.math.BigDecimal;
import java.util.Currency;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BalanceResponse {

    private BigDecimal balance;
    private Currency currency;

}
