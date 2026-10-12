package com.wultra.wallet.entity;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "wallet")
public class WalletEntity {

    @Id
    private UUID id;

    @Column(name = "currency", nullable = false, length = 3)
    private Currency currency;

    private BigDecimal amount;

    private String name;

    private UUID userId;

}
