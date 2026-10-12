package com.wultra.wallet.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.ResourceAccessException;

import com.wultra.wallet.dto.cnb.CnbExRate;
import com.wultra.wallet.dto.cnb.CnbExRatesResponse;
import com.wultra.wallet.entity.WalletEntity;
import com.wultra.wallet.repository.WalletRepository;
import com.wultra.wallet.service.CnbApiClient;
import com.wultra.wallet.service.CurrencyRateService;

import io.restassured.RestAssured;
import io.restassured.config.JsonConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.path.json.config.JsonPathConfig;

/**
 * Example workflow from the assignment, end-to-end over HTTP (real Spring context, H2, mocked CNB).
 * <p>
 * Assignment rates (1.16 and 0.86) are illustrative and not consistent with each other (1 / 1.16 = 0.862).
 * The mock uses CZK rates giving exactly EUR/USD = 1.16, so the USD total matches the assignment (12.80);
 * the EUR total is 11.03 instead of the illustrative 11.02 (7 USD * 0.862 = 6.03, not 7 * 0.86 = 6.02).
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class WalletWorkflowTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency EUR = Currency.getInstance("EUR");

    /** Friday – what CNB returns when asked on a weekend. */
    private static final LocalDate RATES_VALID_FOR = LocalDate.of(2026, 10, 9);

    /** 1 USD = 22.00 CZK, 1 EUR = 25.52 CZK  ->  1 EUR = 25.52 / 22.00 = 1.16 USD. */
    private static final CnbExRatesResponse CNB_RATES = new CnbExRatesResponse(List.of(
            new CnbExRate(RATES_VALID_FOR, 1, "USA", "dollar", 1, "USD", new BigDecimal("22.00")),
            new CnbExRate(RATES_VALID_FOR, 1, "EMU", "euro", 1, "EUR", new BigDecimal("25.52"))));

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private CnbApiClient cnbApiClient;

    private UUID userId;
    private UUID usdWalletId;
    private UUID eurWalletId;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/v1/wallet";
        // read JSON numbers as BigDecimal, not float (12.8f != 12.80)
        RestAssured.config = RestAssuredConfig.config().jsonConfig(
                JsonConfig.jsonConfig().numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL));

        // the rates cache lives in the shared Spring context – start every test with an empty one
        cacheManager.getCache(CurrencyRateService.CNB_RATES_CACHE).clear();
        when(cnbApiClient.getDailyRates(any(LocalDate.class))).thenReturn(CNB_RATES);

        // fresh user per test, independent of data.sql
        userId = UUID.randomUUID();
        usdWalletId = createEmptyWallet(USD);
        eurWalletId = createEmptyWallet(EUR);
    }

    @Test
    void exampleWorkflowFromAssignment() {
        // 1. Deposit 10 USD
        moneyAction("/deposit", usdWalletId, "USD", "10");
        // 2. Deposit 5 EUR
        moneyAction("/deposit", eurWalletId, "EUR", "5");
        // 3. Withdraw 3 USD
        moneyAction("/withdraw", usdWalletId, "USD", "3");

        // 4. Total balance in USD: 7 USD + 5 EUR (* 1.16 = 5.80 USD) = 12.80 USD
        given()
                .when().get("/user/{userId}/currency/{currency}", userId, "USD")
                .then()
                .statusCode(200)
                .body("balance", comparesEqualTo(new BigDecimal("12.80")))
                .body("currency", equalTo("USD"))
                .body("ratesValidForDate", equalTo("2026-10-09"));

        // 5. Total balance in EUR: 5 EUR + 7 USD (* 22.00 / 25.52 = 6.03 EUR) = 11.03 EUR
        given()
                .when().get("/user/{userId}/currency/{currency}", userId, "EUR")
                .then()
                .statusCode(200)
                .body("balance", comparesEqualTo(new BigDecimal("11.03")))
                .body("currency", equalTo("EUR"))
                .body("ratesValidForDate", equalTo("2026-10-09"));

        // steps 4 and 5 used the same cached rate table – CNB was called only once
        verify(cnbApiClient, times(1)).getDailyRates(any(LocalDate.class));
    }

    @Test
    void cnbUnavailableAndNothingCachedReturns503() {
        when(cnbApiClient.getDailyRates(any(LocalDate.class))).thenThrow(new ResourceAccessException("CNB down"));
        moneyAction("/deposit", usdWalletId, "USD", "10");

        given()
                .when().get("/user/{userId}/currency/{currency}", userId, "EUR")
                .then()
                .statusCode(503)
                .body("code", equalTo("RATES_UNAVAILABLE"));
    }

    @Test
    void cnbUnavailableButRatesCachedStillWorks() {
        moneyAction("/deposit", usdWalletId, "USD", "10");
        // first call loads and caches the rates
        given().when().get("/user/{userId}/currency/{currency}", userId, "EUR").then().statusCode(200);

        when(cnbApiClient.getDailyRates(any(LocalDate.class))).thenThrow(new ResourceAccessException("CNB down"));

        given()
                .when().get("/user/{userId}/currency/{currency}", userId, "EUR")
                .then()
                .statusCode(200)
                .body("ratesValidForDate", equalTo("2026-10-09"));
    }

    @Test
    void withdrawMoreThanBalanceIsRejected() {
        moneyAction("/deposit", usdWalletId, "USD", "10");

        given()
                .contentType(ContentType.JSON)
                .body(actionJson(usdWalletId, "USD", "10.01"))
                .when().put("/withdraw")
                .then()
                .statusCode(409)
                .body("code", equalTo("INSUFFICIENT_FUNDS"));
    }

    private void moneyAction(String path, UUID walletId, String currency, String amount) {
        given()
                .contentType(ContentType.JSON)
                .body(actionJson(walletId, currency, amount))
                .when().put(path)
                .then()
                .statusCode(200);
    }

    private static String actionJson(UUID walletId, String currency, String amount) {
        return """
                {"walletId": "%s", "currency": "%s", "amount": %s}
                """.formatted(walletId, currency, amount);
    }

    private UUID createEmptyWallet(Currency currency) {
        WalletEntity wallet = new WalletEntity();
        wallet.setId(UUID.randomUUID());
        wallet.setUserId(userId);
        wallet.setCurrency(currency);
        wallet.setName(currency.getCurrencyCode() + " wallet");
        wallet.setAmount(BigDecimal.ZERO);
        return walletRepository.save(wallet).getId();
    }
}
