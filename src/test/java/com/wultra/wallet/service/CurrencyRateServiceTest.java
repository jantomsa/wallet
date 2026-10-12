package com.wultra.wallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;

import com.wultra.wallet.dto.cnb.CnbExRate;
import com.wultra.wallet.dto.cnb.CnbExRatesResponse;
import com.wultra.wallet.exception.ErrorCode;
import com.wultra.wallet.exception.WultraWalletException;

/** Loading of CNB rate snapshots. Conversion itself is tested in {@link ExchangeRatesTest}. */
@ExtendWith(MockitoExtension.class)
class CurrencyRateServiceTest {

    /** A Friday – what CNB returns when asked on a weekend. */
    private static final LocalDate RATE_DATE = LocalDate.of(2026, 10, 9);

    @Mock
    private CnbApiClient cnbApiClient;

    @InjectMocks
    private CurrencyRateService service;

    @Test
    void loadedRatesCarryCnbValidForDate() {
        CnbExRatesResponse cnbResponse = new CnbExRatesResponse(List.of(
                new CnbExRate(RATE_DATE, 195, "EMU", "euro", 1, "EUR", new BigDecimal("24.32"))));
        when(cnbApiClient.getDailyRates(any(LocalDate.class))).thenReturn(cnbResponse);

        ExchangeRates rates = service.loadCurrentRates();

        assertThat(rates.getValidFor()).isEqualTo(RATE_DATE);
        assertThat(rates.getRates()).hasSize(1);
    }

    @Test
    void cnbUnreachableFailsWithRatesUnavailable() {
        when(cnbApiClient.getDailyRates(any(LocalDate.class))).thenThrow(new ResourceAccessException("timeout"));

        assertRatesUnavailable();
    }

    @Test
    void emptyCnbResponseFailsWithRatesUnavailable() {
        when(cnbApiClient.getDailyRates(any(LocalDate.class))).thenReturn(new CnbExRatesResponse(List.of()));

        assertRatesUnavailable();
    }

    @Test
    void nullCnbResponseFailsWithRatesUnavailable() {
        when(cnbApiClient.getDailyRates(any(LocalDate.class))).thenReturn(null);

        assertRatesUnavailable();
    }

    private void assertRatesUnavailable() {
        assertThatThrownBy(() -> service.loadCurrentRates())
                .isInstanceOf(WultraWalletException.class)
                .extracting(e -> ((WultraWalletException) e).getErrorCode())
                .isEqualTo(ErrorCode.RATES_UNAVAILABLE);
    }
}
