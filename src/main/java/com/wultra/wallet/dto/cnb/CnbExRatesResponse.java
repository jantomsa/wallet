package com.wultra.wallet.dto.cnb;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CnbExRatesResponse(List<CnbExRate> rates) {
}
