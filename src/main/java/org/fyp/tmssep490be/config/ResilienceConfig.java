package org.fyp.tmssep490be.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4J Configuration for Email Service
 * Provides rate limiting and retry mechanisms
 */
@Configuration
@Slf4j
public class ResilienceConfig {

    @Value("${resend.rate.limit:5}")
    private int emailRateLimit;

    @Value("${resend.rate.limit.duration:1}")
    private int emailRateLimitDuration;

    @Value("${resend.retry.max-attempts:3}")
    private int emailRetryMaxAttempts;

    @Value("${resend.retry.delay:1000}")
    private long emailRetryDelay;

    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        RateLimiterConfig emailRateLimiterConfig = RateLimiterConfig.custom()
                .limitForPeriod(emailRateLimit)
                .limitRefreshPeriod(Duration.ofSeconds(emailRateLimitDuration))
                .timeoutDuration(Duration.ofSeconds(5))
                .build();

        return RateLimiterRegistry.of(emailRateLimiterConfig);
    }

    @Bean
    public RateLimiter emailRateLimiter(RateLimiterRegistry registry) {
        RateLimiter rateLimiter = registry.rateLimiter("emailRateLimiter");

        rateLimiter.getEventPublisher()
                .onSuccess(event -> log.debug("Email rate limiter success: {}", event))
                .onFailure(event -> log.warn("Email rate limiter failure: {}", event));
                // .onRateLimitExceeded(event -> log.warn("Email rate limit exceeded: {}", event)); // Method may not exist in this version

        return rateLimiter;
    }

    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig emailRetryConfig = RetryConfig.custom()
                .maxAttempts(emailRetryMaxAttempts)
                .waitDuration(Duration.ofMillis(emailRetryDelay))
                .retryExceptions(Exception.class)
                .retryOnResult(result -> result == null)
                .build();

        return RetryRegistry.of(emailRetryConfig);
    }

    @Bean
    public Retry emailRetry(RetryRegistry registry) {
        Retry retry = registry.retry("emailRetry");

        retry.getEventPublisher()
                .onRetry(event -> log.info("Email retry attempt {}: {}", event.getNumberOfRetryAttempts(), event.getLastThrowable().getMessage()))
                .onSuccess(event -> log.info("Email retry succeeded after {} attempts", event.getNumberOfRetryAttempts()))
                .onError(event -> log.error("Email retry failed after {} attempts: {}", event.getNumberOfRetryAttempts(), event.getLastThrowable().getMessage()));

        return retry;
    }
}