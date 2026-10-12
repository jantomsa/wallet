package com.wultra.wallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.wultra.wallet.dto.ActionRequest;
import com.wultra.wallet.entity.WalletEntity;
import com.wultra.wallet.exception.ErrorCode;
import com.wultra.wallet.exception.WultraWalletException;
import com.wultra.wallet.repository.WalletRepository;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final Currency USD = Currency.getInstance("USD");
    private static final UUID WALLET_ID = UUID.randomUUID();

    @Mock
    private WalletRepository repository;

    @Mock
    private CurrencyRateService currencyRateService;

    @InjectMocks
    private WalletService service;

    private WalletEntity wallet;

    @BeforeEach
    void setUp() {
        wallet = WalletEntity.builder()
                .id(WALLET_ID)
                .currency(EUR)
                .amount(new BigDecimal("10.00"))
                .build();
    }

    @Test
    void withdrawLowerThanBalanceSubtractsAmount() {
        mockWalletFound();

        service.withdraw(input(EUR, "3.50"));

        assertThat(wallet.getAmount()).isEqualByComparingTo("6.50");
    }

    @Test
    void withdrawWholeBalanceLeavesZero() {
        mockWalletFound();

        service.withdraw(input(EUR, "10"));   // different scale than 10.00 on purpose – compareTo, not equals

        assertThat(wallet.getAmount()).isEqualByComparingTo("0");
    }

    @Test
    void withdrawMoreThanBalanceIsRejectedAndBalanceUnchanged() {
        mockWalletFound();

        assertThatThrownBy(() -> service.withdraw(input(EUR, "10.01")))
                .isInstanceOf(WultraWalletException.class)
                .extracting(e -> ((WultraWalletException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_FUNDS);
        assertThat(wallet.getAmount()).isEqualByComparingTo("10.00");
    }

    @Test
    void withdrawFromEmptyWalletIsRejected() {
        wallet.setAmount(BigDecimal.ZERO);
        mockWalletFound();

        assertThatThrownBy(() -> service.withdraw(input(EUR, "0.01")))
                .isInstanceOf(WultraWalletException.class)
                .extracting(e -> ((WultraWalletException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_FUNDS);
        assertThat(wallet.getAmount()).isEqualByComparingTo("0");
    }

    @Test
    void withdrawInDifferentCurrencyIsRejectedAndBalanceUnchanged() {
        mockWalletFound();

        assertThatThrownBy(() -> service.withdraw(input(USD, "1")))
                .isInstanceOf(WultraWalletException.class)
                .extracting(e -> ((WultraWalletException) e).getErrorCode())
                .isEqualTo(ErrorCode.CURRENCY_MISMATCH);
        assertThat(wallet.getAmount()).isEqualByComparingTo("10.00");
    }

    @Test
    void withdrawFromUnknownWalletIsRejected() {
        when(repository.findWithLock(WALLET_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.withdraw(input(EUR, "1")))
                .isInstanceOf(WultraWalletException.class)
                .extracting(e -> ((WultraWalletException) e).getErrorCode())
                .isEqualTo(ErrorCode.ENTITY_NOT_FOUND);
    }

    private void mockWalletFound() {
        when(repository.findWithLock(WALLET_ID)).thenReturn(Optional.of(wallet));
    }

    private static ActionRequest input(Currency currency, String amount) {
        ActionRequest input = new ActionRequest();
        input.setWalletId(WALLET_ID);
        input.setCurrency(currency);
        input.setAmount(new BigDecimal(amount));
        return input;
    }
}
