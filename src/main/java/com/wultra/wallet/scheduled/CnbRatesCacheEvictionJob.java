package com.wultra.wallet.scheduled;

import java.time.LocalDate;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.wultra.wallet.service.CurrencyRateService;
import com.wultra.wallet.service.ExchangeRates;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Drops cached CNB rates when newer ones may be available.
 * <p>
 * CNB declares new rates on working days around 14:30. In that window the job checks the cached table:
 * if it is already valid for today, it is kept (CNB publishes nothing more today); otherwise the cache
 * is cleared and the next request loads the current table from CNB.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CnbRatesCacheEvictionJob {

    private final CacheManager cacheManager;

    @Scheduled(cron = "${cnb.rates.cacheEvictCronFrom1430}")
    public void evictCnbRates() {
        Cache cache = cacheManager.getCache(CurrencyRateService.CNB_RATES_CACHE);
        if (cache == null) {
            return;
        }
        // key of the no-arg @Cacheable method CurrencyRateService.loadCurrentRates()
        ExchangeRates cached = cache.get(SimpleKey.EMPTY, ExchangeRates.class);
        if (cached == null || cached.getValidFor().equals(LocalDate.now())) {
            return;   // nothing cached, or today's rates already cached – CNB publishes nothing more today
        }
        cache.clear();
        log.info("Cached CNB rates are valid for {}, cache cleared to pick up newer rates", cached.getValidFor());
    }
}
