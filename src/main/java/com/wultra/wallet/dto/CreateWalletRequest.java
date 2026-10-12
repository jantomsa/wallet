package com.wultra.wallet.dto;

import java.util.Currency;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "New wallet in a single currency, owned by a user. Starts with balance 0.")
public class CreateWalletRequest {

    @Schema(description = "Owner of the wallet", example = "905a48a6-ffcf-430e-9e92-44bfa08427ac")
    @NotNull
    private UUID userId;

    @Schema(description = "ISO 4217 currency code of the wallet – all deposits/withdrawals must use it",
            implementation = String.class, example = "USD")
    @NotNull
    private Currency currency;

    @Schema(description = "Wallet name", example = "Travel money")
    @NotBlank
    @Size(max = 50)
    private String name;
}
