package com.wultra.wallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.wultra.wallet.client.CnbApiClient;
import com.wultra.wallet.dto.ActionRequest;
import com.wultra.wallet.dto.BalanceResponse;
import com.wultra.wallet.dto.CreateWalletRequest;
import com.wultra.wallet.dto.WalletResponse;
import com.wultra.wallet.dto.cnb.CnbExRate;
import com.wultra.wallet.dto.cnb.CnbExRatesResponse;

/**
 * {@link WalletService#createWallet} and {@link WalletService#getBalance} against the real DB (H2), CNB mocked.
 * Only service methods are used – for setup as well as for verification (no direct repository access).
 * <p>
 * Same Spring configuration as WalletWorkflowTest (RANDOM_PORT + mocked CNB), so the context is reused.
 * No @Transactional on the test class – the service must open (and commit) its own transactions.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class WalletServiceIntegrationTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency EUR = Currency.getInstance("EUR");
    private static final Currency CZK = Currency.getInstance("CZK");

    private static final LocalDate RATES_VALID_FOR = LocalDate.of(2026, 10, 9);

    /** 1 USD = 22.00 CZK, 1 EUR = 25.52 CZK  ->  1 EUR = 1.16 USD. */
    private static final CnbExRatesResponse CNB_RATES = new CnbExRatesResponse(List.of(
            new CnbExRate(RATES_VALID_FOR, 1, "USA", "dollar", 1, "USD", new BigDecimal("22.00")),
            new CnbExRate(RATES_VALID_FOR, 1, "EMU", "euro", 1, "EUR", new BigDecimal("25.52"))));

    @Autowired
    private WalletService walletService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private CnbApiClient cnbApiClient;

    private UUID userId;

    @BeforeEach
    void setUp() {
        cacheManager.getCache(CurrencyRateService.CNB_RATES_CACHE).clear();
        when(cnbApiClient.getDailyRates(any(LocalDate.class))).thenReturn(CNB_RATES);
        userId = UUID.randomUUID();
    }

    // ---------------------------------------------------------------- createWallet

    /** The response describes the new empty wallet of the user. */
    @Test
    void createWalletReturnsEmptyWalletOfUser() {
        WalletResponse created = walletService.createWallet(createRequest(USD, "Travel money"));

        assertThat(created.getId()).isNotNull();
        assertThat(created.getUserId()).isEqualTo(userId);
        assertThat(created.getCurrency()).isEqualTo(USD);
        assertThat(created.getName()).isEqualTo("Travel money");
        assertThat(created.getAmount()).isEqualByComparingTo("0");
    }

    /** The created wallet is stored: it counts into the user's balance and money can be deposited into it. */
    @Test
    void createdWalletIsStoredAndUsable() {
        WalletResponse created = walletService.createWallet(createRequest(USD, "Travel money"));
        assertThat(walletService.getBalance(userId, USD).getBalance()).isEqualByComparingTo("0");

        walletService.deposit(depositRequest(created.getId(), USD, "10.50"));

        assertThat(walletService.getBalance(userId, USD).getBalance()).isEqualByComparingTo("10.50");
    }

    // ---------------------------------------------------------------- getBalance

    /** 7 USD + 5 EUR (* 1.16 = 5.80 USD) = 12.80 USD, with the CNB date of the rates used. */
    @Test
    void balanceSumsAllUserWalletsInTargetCurrency() {
        createWalletWithBalance(USD, "7");
        createWalletWithBalance(EUR, "5");

        BalanceResponse balance = walletService.getBalance(userId, USD);

        assertThat(balance.getBalance()).isEqualTo(new BigDecimal("12.80"));   // also checks scale 2
        assertThat(balance.getCurrency()).isEqualTo(USD);
        assertThat(balance.getRatesValidForDate()).isEqualTo(RATES_VALID_FOR);
    }

    /** Wallets of other users are not counted. */
    @Test
    void balanceCountsOnlyWalletsOfTheUser() {
        createWalletWithBalance(EUR, "5");
        UUID otherUser = UUID.randomUUID();
        walletService.createWallet(createRequest(otherUser, EUR, "Someone else"));

        BalanceResponse balance = walletService.getBalance(userId, CZK);

        // 5 EUR * 25.52 = 127.60 CZK
        assertThat(balance.getBalance()).isEqualTo(new BigDecimal("127.60"));
    }

    /** A user without wallets has balance 0.00 – not an error. */
    @Test
    void userWithoutWalletsHasZeroBalance() {
        BalanceResponse balance = walletService.getBalance(userId, EUR);

        assertThat(balance.getBalance()).isEqualTo(new BigDecimal("0.00"));
        assertThat(balance.getRatesValidForDate()).isEqualTo(RATES_VALID_FOR);
    }

    /**
     * The CNB call must not run inside a DB transaction (it would hold a pooled connection for the whole HTTP call).
     * The mocked client records whether a transaction was active at the moment it was called.
     */
    @Test
    void cnbIsCalledOutsideOfTransaction() {
        createWalletWithBalance(EUR, "5");
        AtomicBoolean transactionActiveDuringCnbCall = new AtomicBoolean(true);
        when(cnbApiClient.getDailyRates(any(LocalDate.class))).thenAnswer(invocation -> {
            transactionActiveDuringCnbCall.set(TransactionSynchronizationManager.isActualTransactionActive());
            return CNB_RATES;
        });

        walletService.getBalance(userId, USD);

        assertThat(transactionActiveDuringCnbCall).isFalse();
    }

    // ---------------------------------------------------------------- helpers

    private void createWalletWithBalance(Currency currency, String amount) {
        WalletResponse wallet = walletService.createWallet(createRequest(currency, currency.getCurrencyCode()));
        walletService.deposit(depositRequest(wallet.getId(), currency, amount));
    }

    private static ActionRequest depositRequest(UUID walletId, Currency currency, String amount) {
        ActionRequest deposit = new ActionRequest();
        deposit.setWalletId(walletId);
        deposit.setCurrency(currency);
        deposit.setAmount(new BigDecimal(amount));
        return deposit;
    }

    private CreateWalletRequest createRequest(Currency currency, String name) {
        return createRequest(userId, currency, name);
    }

    private static CreateWalletRequest createRequest(UUID owner, Currency currency, String name) {
        CreateWalletRequest request = new CreateWalletRequest();
        request.setUserId(owner);
        request.setCurrency(currency);
        request.setName(name);
        return request;
    }
}
