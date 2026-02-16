package com.hoang.crypto.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimitConfig {

    @Bean(name = "priceLimitBucket")
    public Bucket priceLimitBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(30)
                .refillIntervally(30, Duration.ofMinutes(1))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    @Bean(name = "tradeLimitBucket")
    public Bucket tradeLimitBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(30)
                .refillIntervally(30, Duration.ofMinutes(1))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
