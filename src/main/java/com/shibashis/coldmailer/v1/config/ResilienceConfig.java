package com.shibashis.coldmailer.v1.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ResilienceConfig {

    @Bean
    public CircuitBreakerRegistry smtpCircuitBreakerRegistry(
            @Value("${app.smtp.cb.failure-rate-threshold:50}") float failureRateThreshold,
            @Value("${app.smtp.cb.sliding-window-size:20}") int slidingWindowSize,
            @Value("${app.smtp.cb.minimum-number-of-calls:10}") int minimumNumberOfCalls,
            @Value("${app.smtp.cb.wait-duration-open-seconds:30}") long waitDurationOpenSeconds) {

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(failureRateThreshold)
                .slidingWindowSize(slidingWindowSize)
                .minimumNumberOfCalls(minimumNumberOfCalls)
                .waitDurationInOpenState(Duration.ofSeconds(waitDurationOpenSeconds))
                .build();

        return CircuitBreakerRegistry.of(config);
    }
}
