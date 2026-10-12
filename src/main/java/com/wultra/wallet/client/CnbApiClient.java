package com.wultra.wallet.client;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.wultra.wallet.dto.cnb.CnbExRatesResponse;

@Component
public class CnbApiClient {

    private static final String DAILY_RATES_PATH = "/cnbapi/exrates/daily";

    private final RestClient restClient;

    public CnbApiClient(@Value("${cnb.api.url}") String baseUrl,
                        @Value("${cnb.api.timeoutInSeconds}") long timeoutInSeconds) {
        Duration timeout = Duration.ofSeconds(timeoutInSeconds);

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public CnbExRatesResponse getDailyRates(LocalDate date) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(DAILY_RATES_PATH)
                        .queryParam("date", DateTimeFormatter.ISO_LOCAL_DATE.format(date))
                        .queryParam("lang", "EN")
                        .build())
                .retrieve()
                .body(CnbExRatesResponse.class);
    }
}
