package com.wultra.wallet.controller;

import java.util.Currency;
import java.util.UUID;

import org.jspecify.annotations.NonNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wultra.wallet.dto.BalanceResponse;
import com.wultra.wallet.dto.ActionInputDTO;
import com.wultra.wallet.service.WalletService;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/v1/wallet")
public class WalletControllerV1 {

    private final WalletService service;

    public WalletControllerV1(WalletService service) {
        this.service = service;
    }

    @GetMapping("/user/{userId}/currency/{currency}")
    public BalanceResponse getTotalBalance(@PathVariable @NonNull UUID userId,
            @PathVariable @NonNull Currency currency) {
        return service.getBalance(userId, currency);
    }

    @PutMapping("/deposit")
    public void deposit(@RequestBody @Valid ActionInputDTO input) {
        service.deposit(input);
    }

    @PutMapping("/withdraw")
    public void withdraw(@RequestBody @Valid ActionInputDTO inputDTO) {
        service.withdraw(inputDTO);
    }

}
