package com.wultra.wallet.service;

import java.math.BigDecimal;


record CzkRate(BigDecimal rate, BigDecimal units) {

    static final CzkRate CZK = new CzkRate(BigDecimal.ONE, BigDecimal.ONE);
}
