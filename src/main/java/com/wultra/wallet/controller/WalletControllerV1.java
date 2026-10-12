package com.wultra.wallet.controller;

import java.util.Currency;
import java.util.UUID;

import org.jspecify.annotations.NonNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.wultra.wallet.dto.BalanceResponse;
import com.wultra.wallet.dto.ActionInputDTO;
import com.wultra.wallet.dto.CreateWalletRequest;
import com.wultra.wallet.dto.WalletResponse;
import com.wultra.wallet.service.WalletService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/v1/wallet")
@RequiredArgsConstructor
public class WalletControllerV1 {

    private final WalletService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WalletResponse createWallet(@RequestBody @Valid CreateWalletRequest request) {
        return service.createWallet(request);
    }

    @GetMapping("/user/{userId}/currency/{targetCurrency}")
    public BalanceResponse getTotalBalance(@PathVariable @NonNull UUID userId,
            @PathVariable @NonNull Currency targetCurrency) {
        return service.getBalance(userId, targetCurrency);
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
