package com.wultra.wallet.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.wultra.wallet.dto.cnb.CnbExRate;
import com.wultra.wallet.dto.cnb.CnbExRatesResponse;
import com.wultra.wallet.exception.ErrorCode;
import com.wultra.wallet.exception.WultraWalletException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CurrencyRateService {

    private static final Currency CZK = Currency.getInstance("CZK");

    private final CnbApiClient cnbApiClient;

    @Value("${application.globalRoundingMode}")
    private RoundingMode globalRoundingMode;

    @Value("${application.globalNumberScale}")
    private int globalRoundingScale;


    public BigDecimal convertAmount(Currency walletCurrency, Currency targetCurrency, BigDecimal amount) {
        if (walletCurrency.equals(targetCurrency)) {
            return amount;
        }
        List<CnbExRate> rates = loadRates(LocalDate.now());
        CzkRate from = czkRate(walletCurrency, rates);
        CzkRate to = czkRate(targetCurrency, rates);

        // Always transfer to CZK, rates are against CZK
        BigDecimal amountInCzk = amount.multiply(from.rate())
                .divide(from.units(), globalRoundingScale, globalRoundingMode);

        // Now from CZK to target
        return amountInCzk.multiply(to.units())
                .divide(to.rate(), globalRoundingScale, globalRoundingMode);
    }

    private List<CnbExRate> loadRates(LocalDate date) {
        CnbExRatesResponse response = cnbApiClient.getDailyRates(date);
        if (response == null || response.rates() == null || response.rates().isEmpty()) {
            throw new IllegalStateException("CNB returned no exchange rates for " + date);
        }
        return response.rates();
    }

    private CzkRate czkRate(Currency currency, List<CnbExRate> rates) {
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
