package com.nordia.rooms.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Instant;
import java.time.ZoneId;

@TestConfiguration
public class TestClockConfig {

    @Bean
    @Primary
    public MutableClock testClock(@Value("${app.timezone}") String zone) {
        return new MutableClock(Instant.EPOCH, ZoneId.of(zone));
    }
}