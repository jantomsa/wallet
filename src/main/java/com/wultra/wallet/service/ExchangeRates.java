package com.wultra.wallet.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;

import com.wultra.wallet.dto.CzkRate;
import com.wultra.wallet.dto.cnb.CnbExRate;
import com.wultra.wallet.exception.ErrorCode;
import com.wultra.wallet.exception.WultraWalletException;

import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
public class ExchangeRates {

    private static final Currency CZK = Currency.getInstance("CZK");

    private final LocalDate validFor;
    private final List<CnbExRate> rates;


    public BigDecimal convert(BigDecimal amount, Currency from, Currency to) {
        if (from.equals(to)) {
            return amount;
        }
        CzkRate fromRate = czkRate(from);
        CzkRate toRate = czkRate(to);

        // Always transfer to CZK, rates are against CZK
        BigDecimal amountInCzk = amount.multiply(fromRate.rate())
                .divide(fromRate.units(), MathContext.DECIMAL128);

        // Now from CZK to target
        return amountInCzk.multiply(toRate.units())
                .divide(toRate.rate(), MathContext.DECIMAL128);
    }

    private CzkRate czkRate(Currency currency) {
        if (CZK.equals(currency)) {
            return CzkRate.CZK;
        }
        return rates.stream()
                .filter(it -> currency.getCurrencyCode().equals(it.getCurrencyCode()))
                .findFirst()
                .map(it -> new CzkRate(it.getRate(), BigDecimal.valueOf(it.getAmount())))
                .orElseThrow(() -> new WultraWalletException(
                        "CNB does not publish an exchange rate for " + currency.getCurrencyCode(),
                        ErrorCode.UNSUPPORTED_CURRENCY));
    }
}
