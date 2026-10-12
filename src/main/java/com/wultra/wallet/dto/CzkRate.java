package com.wultra.wallet.dto;

import java.math.BigDecimal;


public record CzkRate(BigDecimal rate, BigDecimal units) {

    public static final CzkRate CZK = new CzkRate(BigDecimal.ONE, BigDecimal.ONE);
}
