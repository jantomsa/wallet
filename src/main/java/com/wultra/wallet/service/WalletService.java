package com.wultra.wallet.service;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.wultra.wallet.dto.BalanceResponse;
import com.wultra.wallet.dto.ActionInputDTO;


@Service
public class WalletService {

    public BalanceResponse getBalance(UUID userId, Currency currency) {
        /// in progress
        return new BalanceResponse(BigDecimal.ZERO, currency);
    }

    public void deposit(ActionInputDTO input) {
        /// in progress
    }

    public void withdraw(ActionInputDTO inputDTO) {
        /// in progress
    }
}
