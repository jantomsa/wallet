package com.wultra.wallet.service;

import java.time.LocalDate;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import com.wultra.wallet.dto.cnb.CnbExRatesResponse;
import com.wultra.wallet.exception.ErrorCode;
import com.wultra.wallet.exception.WultraWalletException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CurrencyRateService {

    public static final String CNB_RATES_CACHE = "cnbRates";

    private final CnbApiClient cnbApiClient;

    @Cacheable(CNB_RATES_CACHE)
    public ExchangeRates loadCurrentRates() {
        LocalDate today = LocalDate.now();
        CnbExRatesResponse response;
        try {
            response = cnbApiClient.getDailyRates(today);
        } catch (RestClientException e) {
            log.error("CNB exchange rates could not be loaded for {}", today, e);
            throw ratesUnavailable();
        }
        if (response == null || response.rates() == null || response.rates().isEmpty()) {
            log.error("CNB returned no exchange rates for {}", today);
            throw ratesUnavailable();
        }
        log.info("Loaded CNB exchange rates valid for {}", response.rates().getFirst().getValidFor());
        // all rows of one CNB table share the same validFor
        return new ExchangeRates(response.rates().getFirst().getValidFor(), response.rates());
    }

    private static WultraWalletException ratesUnavailable() {
        return new WultraWalletException("Exchange rates are temporarily unavailable, balance cannot be converted. "
                + "Please try again later.", ErrorCode.RATES_UNAVAILABLE);
    }
}
