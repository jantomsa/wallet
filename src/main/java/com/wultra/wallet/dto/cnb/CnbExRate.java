package com.wultra.wallet.dto.cnb;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CnbExRate {

    private LocalDate validFor;
    private int order;
    private String country;
    private String currency;
    private int amount;
    private String currencyCode;
    private BigDecimal rate;
}
