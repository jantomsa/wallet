package com.wultra.wallet.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.wultra.wallet.dto.ActionInputDTO;
import com.wultra.wallet.entity.WalletEntity;
import com.wultra.wallet.exception.ErrorCode;
import com.wultra.wallet.exception.WultraWalletException;
import com.wultra.wallet.repository.WalletRepository;


@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ConcurrentMoneyOperationsTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final int THREADS = 10;

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    /** Not used here (no conversion), present only to share the Spring context with WalletWorkflowTest. */
    @MockitoBean
    private CnbApiClient cnbApiClient;

    /**
     * Balance 50.00, 50 threads withdraw 1.00 at the same moment.
     * The row lock serializes them: each one sees the balance left by the previous one,
     * so exactly 5 succeed, 5 get INSUFFICIENT_FUNDS and the balance ends at exactly 0.00.
     */
    @Test
    void simultaneousWithdrawalsNeverOverdrawTheBalance() throws Exception {
        // balance 5.00, 10 threads try to withdraw 1.00 at the same moment
        UUID walletId = createWallet("50.00");

        List<Boolean> results = runSimultaneously(55, () -> {
            try {
                walletService.withdraw(input(walletId, "1.00"));
                return true;
            } catch (WultraWalletException e) {
                assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_FUNDS);
                return false;
            }
        });

        // exactly 50 succeed, 5 are rejected, the balance is exactly 0 – never negative, no lost update
        assertThat(results).filteredOn(Boolean::booleanValue).hasSize(50);
        assertThat(results).filteredOn(ok -> !ok).hasSize(5);
        assertThat(balanceOf(walletId)).isEqualByComparingTo("0.00");
    }

    /**
     * Balance 0.00, 10 threads deposit 1.00 at the same moment.
     * Without a lock two deposits could read the same balance and one of them would be lost (lost update).
     */
    @Test
    void simultaneousDepositsAreAllApplied() throws Exception {
        // without a lock two deposits could read the same balance and one of them would be lost
        UUID walletId = createWallet("0.00");

        runSimultaneously(100, () -> {
            walletService.deposit(input(walletId, "1.00"));
            return true;
        });

        assertThat(balanceOf(walletId)).isEqualByComparingTo("100.00");
    }

    /**
     * Runs the task in {@code threads} threads at once (one thread per task, so all start immediately)
     * and waits for all of them. Unexpected exceptions (deadlock, lock timeout) are rethrown by {@code get()}.
     */
    private static <T> List<T> runSimultaneously(int threads, Callable<T> task) throws Exception {
        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            List<Future<T>> futures = pool.invokeAll(Collections.nCopies(threads, task), 30, TimeUnit.SECONDS);
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get());
            }
            return results;
        }
    }

    private UUID createWallet(String amount) {
        WalletEntity wallet = new WalletEntity();
        wallet.setId(UUID.randomUUID());
        wallet.setUserId(UUID.randomUUID());
        wallet.setCurrency(EUR);
        wallet.setName("concurrency test");
        wallet.setAmount(new BigDecimal(amount));
        return walletRepository.save(wallet).getId();
    }

    private BigDecimal balanceOf(UUID walletId) {
        return walletRepository.findById(walletId).orElseThrow().getAmount();
    }

    private static ActionInputDTO input(UUID walletId, String amount) {
        ActionInputDTO input = new ActionInputDTO();
        input.setWalletId(walletId);
        input.setCurrency(EUR);
        input.setAmount(new BigDecimal(amount));
        return input;
    }
}
