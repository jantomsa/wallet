package com.wultra.wallet.scheduled;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.SimpleKey;

import com.wultra.wallet.service.CurrencyRateService;
import com.wultra.wallet.service.ExchangeRates;

class CnbRatesCacheEvictionJobTest {

    private Cache cache;
    private CnbRatesCacheEvictionJob job;

    @BeforeEach
    void setUp() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(CurrencyRateService.CNB_RATES_CACHE);
        cache = cacheManager.getCache(CurrencyRateService.CNB_RATES_CACHE);
        job = new CnbRatesCacheEvictionJob(cacheManager);
    }

    @Test
    void todaysRatesStayCached() {
        cache.put(SimpleKey.EMPTY, new ExchangeRates(LocalDate.now(), List.of()));

        job.evictCnbRates();

        assertThat(cache.get(SimpleKey.EMPTY)).isNotNull();
    }

    @Test
    void outdatedRatesAreEvicted() {
        cache.put(SimpleKey.EMPTY, new ExchangeRates(LocalDate.now().minusDays(1), List.of()));

        job.evictCnbRates();

        assertThat(cache.get(SimpleKey.EMPTY)).isNull();
    }

    @Test
    void emptyCacheIsNoOp() {
        job.evictCnbRates();

        assertThat(cache.get(SimpleKey.EMPTY)).isNull();
    }
}
