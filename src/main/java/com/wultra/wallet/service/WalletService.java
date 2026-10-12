package com.wultra.wallet.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wultra.wallet.dto.BalanceResponse;
import com.wultra.wallet.dto.ActionRequest;
import com.wultra.wallet.dto.CreateWalletRequest;
import com.wultra.wallet.dto.WalletResponse;
import com.wultra.wallet.entity.WalletEntity;
import com.wultra.wallet.exception.ErrorCode;
import com.wultra.wallet.exception.WultraWalletException;
import com.wultra.wallet.repository.WalletRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository repository;
    private final CurrencyRateService currencyRateService;

    @Value("${application.globalRoundingMode}")
    private RoundingMode globalRoundingMode;

    @Value("${application.globalNumberScale}")
    private int globalRoundingScale;

    private WalletEntity getWallet(ActionRequest input) {
        WalletEntity wallet = repository.findWithLock(input.getWalletId())
                .orElseThrow(() -> new WultraWalletException("Wallet entity not found by id: " + input.getWalletId(),
                        ErrorCode.ENTITY_NOT_FOUND));
        verifySameCurrency(wallet, input.getCurrency());
        return wallet;
    }

    @Transactional
    public WalletResponse createWallet(CreateWalletRequest request) {
        WalletEntity wallet = repository.save(WalletEntity.builder()
                .id(UUID.randomUUID())
                .userId(request.getUserId())
                .currency(request.getCurrency())
                .name(request.getName())
                .amount(BigDecimal.ZERO)
                .build());
        log.info("Created {} wallet {} for user {}", wallet.getCurrency(), wallet.getId(), wallet.getUserId());

        return WalletResponse.builder()
                .id(wallet.getId())
                .userId(wallet.getUserId())
                .currency(wallet.getCurrency())
                .name(wallet.getName())
                .amount(wallet.getAmount())
                .build();
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(UUID userId, Currency targetCurrency) {
        List<WalletEntity> wallets = repository.findByUserId(userId);
        ExchangeRates rates = currencyRateService.loadCurrentRates();

        BigDecimal total = wallets.stream()
                .map(it -> rates.convert(it.getAmount(), it.getCurrency(), targetCurrency))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(globalRoundingScale, globalRoundingMode);

        return new BalanceResponse(total, targetCurrency, rates.getValidFor());
    }

    @Transactional
    public void deposit(ActionRequest input) {
        log.info("Making deposit with input: {}", input);
        WalletEntity wallet = getWallet(input);
        wallet.setAmount(wallet.getAmount().add(input.getAmount()));
    }

    @Transactional
    public void withdraw(ActionRequest input) {
        log.info("Making withdraw with input: {}", input);
        WalletEntity wallet = getWallet(input);
        verifySufficientFunds(wallet, input.getAmount());
        wallet.setAmount(wallet.getAmount().subtract(input.getAmount()));
    }

    private void verifySufficientFunds(WalletEntity wallet, BigDecimal amount) {
        if (wallet.getAmount().compareTo(amount) < 0) {
            throw new WultraWalletException("Insufficient funds in wallet '" + wallet.getId() + "'",
                    ErrorCode.INSUFFICIENT_FUNDS);
        }
    }

    private void verifySameCurrency(WalletEntity wallet, Currency requestCurrency) {
        if (!wallet.getCurrency().equals(requestCurrency)) {
            throw new WultraWalletException("Currency '" + requestCurrency.getCurrencyCode()
                    + "' does not match wallet currency '" + wallet.getCurrency().getCurrencyCode()
                    + "'. Money is not converted on deposit/withdraw, currency must match.",
                    ErrorCode.CURRENCY_MISMATCH);
        }
    }
}
