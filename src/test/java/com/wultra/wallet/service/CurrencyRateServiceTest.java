package com.wultra.wallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.wultra.wallet.dto.cnb.CnbExRate;
import com.wultra.wallet.dto.cnb.CnbExRatesResponse;
import com.wultra.wallet.exception.ErrorCode;
import com.wultra.wallet.exception.WultraWalletException;

@ExtendWith(MockitoExtension.class)
class CurrencyRateServiceTest {

    private static final Currency CZK = Currency.getInstance("CZK");
    private static final Currency EUR = Currency.getInstance("EUR");
    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency GBP = Currency.getInstance("GBP");

    private static final LocalDate RATE_DATE = LocalDate.of(2019, 5, 17);

    /** Real CNB values from 2019-05-17. CZK is not part of the CNB table (rates are quoted against CZK). */
    private static final CnbExRatesResponse CNB_RATES = new CnbExRatesResponse(List.of(
            new CnbExRate(RATE_DATE, 94, "EMU", "euro", 1, "EUR", new BigDecimal("25.75")),
            new CnbExRate(RATE_DATE, 94, "USA", "dolar", 1, "USD", new BigDecimal("23.048"))));

    @Mock
    private CnbApiClient cnbApiClient;

    private CurrencyRateService service;

    @BeforeEach
    void setUp() {
        service = new CurrencyRateService(cnbApiClient);
        ReflectionTestUtils.setField(service, "globalRoundingMode", RoundingMode.HALF_EVEN);
        ReflectionTestUtils.setField(service, "globalRoundingScale", 2);
    }

    @Test
    void convertsCzkToUsd() {
        mockCnbRates(CNB_RATES);

        // 100 / 23.048 = 4.33877...
        assertThat(service.convertAmount(CZK, USD, new BigDecimal("100")))
                .isEqualTo(new BigDecimal("4.34"));
    }

    @Test
    void convertsUsdToCzk() {
        mockCnbRates(CNB_RATES);

        // 10 * 23.048 = 230.48
        assertThat(service.convertAmount(USD, CZK, new BigDecimal("10")))
                .isEqualTo(new BigDecimal("230.48"));
    }

    @Test
    void convertsUsdToEurViaCzk() {
        mockCnbRates(CNB_RATES);

        // 10 USD = 230.48 CZK; 230.48 / 25.75 = 8.95068...
        assertThat(service.convertAmount(USD, EUR, new BigDecimal("10")))
                .isEqualTo(new BigDecimal("8.95"));
    }

    @Test
    void convertsEurToUsdViaCzk() {
        mockCnbRates(CNB_RATES);

        // 5 EUR = 128.75 CZK; 128.75 / 23.048 = 5.58616...
        assertThat(service.convertAmount(EUR, USD, new BigDecimal("5")))
                .isEqualTo(new BigDecimal("5.59"));
    }

    @Test
    void sameCurrencyReturnsAmountWithoutCallingCnb() {
        BigDecimal amount = new BigDecimal("12.3456");

        assertThat(service.convertAmount(USD, USD, amount)).isSameAs(amount);
        verifyNoInteractions(cnbApiClient);
    }

    @Test
    void currencyNotPublishedByCnbIsRejected() {
        mockCnbRates(CNB_RATES);

        assertThatThrownBy(() -> service.convertAmount(CZK, GBP, BigDecimal.TEN))
                .isInstanceOf(WultraWalletException.class)
                .extracting(e -> ((WultraWalletException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNSUPPORTED_CURRENCY);
    }

    @Test
    void emptyCnbResponseFails() {
        mockCnbRates(new CnbExRatesResponse(List.of()));

        assertThatThrownBy(() -> service.convertAmount(CZK, USD, BigDecimal.TEN))
                .isInstanceOf(IllegalStateException.class);
    }

    private void mockCnbRates(CnbExRatesResponse response) {
        when(cnbApiClient.getDailyRates(any(LocalDate.class))).thenReturn(response);
    }
}
