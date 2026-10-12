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

import com.wultra.wallet.dto.cnb.CnbExRate;
import com.wultra.wallet.dto.cnb.CnbExRatesResponse;

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
    void emptyCnbResponseFails() {
        when(cnbApiClient.getDailyRates(any(LocalDate.class))).thenReturn(new CnbExRatesResponse(List.of()));

        assertThatThrownBy(() -> service.loadCurrentRates()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void nullCnbResponseFails() {
        when(cnbApiClient.getDailyRates(any(LocalDate.class))).thenReturn(null);

        assertThatThrownBy(() -> service.loadCurrentRates()).isInstanceOf(IllegalStateException.class);
    }
}
