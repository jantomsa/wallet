package com.wultra.wallet.config;

import java.time.ZoneId;
import java.util.TimeZone;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Configuration
public class TimeZoneConfig {

    @Value("${application.timezone}")
    private String timezone;

    @PostConstruct
    void setDefaultTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of(timezone)));
        log.info("Application time zone set to {}", TimeZone.getDefault().getID());
    }
}
