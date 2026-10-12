package com.wultra.wallet.dto;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
@Schema(description = "Wallet in a single currency")
public class WalletResponse {

    @Schema(description = "Wallet id – use it for deposit/withdraw", example = "fcb58bd7-f09a-47f2-a92d-89e20095a4da")
    private UUID id;

    @Schema(description = "Owner of the wallet", example = "905a48a6-ffcf-430e-9e92-44bfa08427ac")
    private UUID userId;

    @Schema(description = "Currency of the wallet (ISO 4217)", implementation = String.class, example = "USD")
    private Currency currency;

    @Schema(description = "Wallet name", example = "Travel money")
    private String name;

    @Schema(description = "Current balance in the wallet currency", example = "0.00")
    private BigDecimal amount;
}
