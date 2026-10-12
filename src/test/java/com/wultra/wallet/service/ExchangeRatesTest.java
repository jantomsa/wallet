package com.wultra.wallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.wultra.wallet.dto.cnb.CnbExRate;
import com.wultra.wallet.exception.ErrorCode;
import com.wultra.wallet.exception.WultraWalletException;

/** Pure conversion logic – no Spring, no mocks. */
class ExchangeRatesTest {

    private static final Currency CZK = Currency.getInstance("CZK");
    private static final Currency EUR = Currency.getInstance("EUR");
    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency JPY = Currency.getInstance("JPY");
    private static final Currency GBP = Currency.getInstance("GBP");

    private static final LocalDate RATE_DATE = LocalDate.of(2019, 5, 17);

    /** Real CNB values from 2019-05-17. CZK is not part of the CNB table (rates are quoted against CZK). */
    private static final ExchangeRates RATES = new ExchangeRates(RATE_DATE, List.of(
            new CnbExRate(RATE_DATE, 94, "EMU", "euro", 1, "EUR", new BigDecimal("25.75")),
            new CnbExRate(RATE_DATE, 94, "USA", "dolar", 1, "USD", new BigDecimal("23.048")),
            new CnbExRate(RATE_DATE, 94, "Japonsko", "jen", 100, "JPY", new BigDecimal("21.045"))));

    @Test
    void convertsCzkToUsd() {
        // 100 / 23.048 = 4.33877...
        assertThat(rounded(RATES.convert(new BigDecimal("100"), CZK, USD))).isEqualTo(new BigDecimal("4.34"));
    }

    @Test
    void convertsUsdToCzk() {
        // 10 * 23.048 = 230.48 exactly
        assertThat(RATES.convert(new BigDecimal("10"), USD, CZK)).isEqualByComparingTo("230.48");
    }

    @Test
    void convertsUsdToEurViaCzk() {
        // 10 USD = 230.48 CZK; 230.48 / 25.75 = 8.95068...
        assertThat(rounded(RATES.convert(new BigDecimal("10"), USD, EUR))).isEqualTo(new BigDecimal("8.95"));
    }

    @Test
    void convertsEurToUsdViaCzk() {
        // 5 EUR = 128.75 CZK; 128.75 / 23.048 = 5.58616...
        assertThat(rounded(RATES.convert(new BigDecimal("5"), EUR, USD))).isEqualTo(new BigDecimal("5.59"));
    }

    @Test
    void intermediateCzkAmountIsNotRounded() {
        // 72 * 23.048 / 25.75 = 64.4449... -> 64.44.
        // Rounding the CZK amount first (1659.456 -> 1659.46) would give 64.45.
        assertThat(rounded(RATES.convert(new BigDecimal("72"), USD, EUR))).isEqualTo(new BigDecimal("64.44"));
    }

    @Test
    void handlesRateQuotedPer100Units() {
        // JPY is quoted per 100 units: 100 JPY = 21.045 CZK -> 1000 CZK = 1000 * 100 / 21.045 = 4751.72 JPY
        assertThat(rounded(RATES.convert(new BigDecimal("1000"), CZK, JPY))).isEqualTo(new BigDecimal("4751.72"));
        // and back: 200 JPY = 200 * 21.045 / 100 = 42.09 CZK
        assertThat(RATES.convert(new BigDecimal("200"), JPY, CZK)).isEqualByComparingTo("42.09");
    }

    @Test
    void sameCurrencyReturnsAmountUnchanged() {
        BigDecimal amount = new BigDecimal("12.3456");

        assertThat(RATES.convert(amount, USD, USD)).isSameAs(amount);
    }

    @Test
    void currencyNotPublishedByCnbIsRejected() {
        assertThatThrownBy(() -> RATES.convert(BigDecimal.TEN, CZK, GBP))
                .isInstanceOf(WultraWalletException.class)
                .extracting(e -> ((WultraWalletException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNSUPPORTED_CURRENCY);
    }

    private static BigDecimal rounded(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
