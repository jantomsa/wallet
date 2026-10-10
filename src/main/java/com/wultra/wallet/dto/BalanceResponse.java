package com.wultra.wallet.dto;

import java.math.BigDecimal;
import java.util.Currency;

public class BalanceResponse {

    private BigDecimal balance;
    private Currency currency;

    public BalanceResponse(BigDecimal balance, Currency currency) {
        this.balance = balance;
        this.currency = currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }
}
