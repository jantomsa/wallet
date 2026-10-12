package com.wultra.wallet.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables in-memory caching (Spring's default ConcurrentMap cache) and scheduled jobs.
 * CNB rates cache refresh: see {@link com.wultra.wallet.scheduled.CnbRatesCacheEvictionJob}.
 */
@Configuration
@EnableCaching
@EnableScheduling
public class CacheConfig {
}
