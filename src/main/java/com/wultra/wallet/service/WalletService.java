package com.wultra.wallet.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wultra.wallet.dto.BalanceResponse;
import com.wultra.wallet.dto.ActionInputDTO;
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

    private WalletEntity getWallet(ActionInputDTO input) {
        WalletEntity wallet = repository.findWithLock(input.getWalletId())
                .orElseThrow(() -> new WultraWalletException("Wallet entity not found by id: " + input.getWalletId(),
                        ErrorCode.ENTITY_NOT_FOUND));
        verifySameCurrency(wallet, input.getCurrency());
        return wallet;
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(UUID userId, Currency targetCurrency) {
        List<WalletEntity> wallets = repository.findByUserId(userId);
        BigDecimal balanceInCurrency = wallets.stream().map(it ->
                        currencyRateService.convertAmount(it.getCurrency(), targetCurrency, it.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new BalanceResponse(balanceInCurrency, targetCurrency);
    }

    /// todo - Approach will be verified in test, desired thing is Pessimistic lock handled in DB
    @Transactional
    public void deposit(ActionInputDTO input) {
        log.info("Making deposit with input: {}", input);
        WalletEntity wallet = getWallet(input);
        wallet.setAmount(wallet.getAmount().add(input.getAmount()));
    }

    @Transactional
    public void withdraw(ActionInputDTO input) {
        log.info("Making withdraw with input: {}", input);
        WalletEntity wallet = getWallet(input);
        wallet.setAmount(wallet.getAmount().subtract(input.getAmount()));
    }

    private void verifySameCurrency(WalletEntity wallet, Currency requestCurrency) {
        if (!wallet.getCurrency().equals(requestCurrency)) {
            throw new WultraWalletException("Currency " + requestCurrency.getCurrencyCode()
                    + " does not match wallet currency " + wallet.getCurrency().getCurrencyCode()
                    + "; money is not converted on deposit/withdraw",
                    ErrorCode.CURRENCY_MISMATCH);
        }
    }
}
