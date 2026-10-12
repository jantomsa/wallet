package com.wultra.wallet.service;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.wultra.wallet.dto.cnb.CnbExRatesResponse;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CurrencyRateService {

    private final CnbApiClient cnbApiClient;

    public ExchangeRates loadCurrentRates() {
        LocalDate today = LocalDate.now();
        CnbExRatesResponse response = cnbApiClient.getDailyRates(today);
        if (response == null || response.rates() == null || response.rates().isEmpty()) {
            throw new IllegalStateException("CNB returned no exchange rates for " + today);
        }
        // all rows of one CNB table share the same validFor
        return new ExchangeRates(response.rates().getFirst().getValidFor(), response.rates());
    }
}
