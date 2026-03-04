package com.shibashis.coldmailer.v1.services;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class WorkerMetricsService {

    private final AtomicLong processed = new AtomicLong();
    private final AtomicLong sent = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();
    private volatile LocalDateTime lastProcessedAt;

    public void markProcessed() {
        processed.incrementAndGet();
        lastProcessedAt = LocalDateTime.now();
    }

    public void markSent() {
        sent.incrementAndGet();
    }

    public void markFailed() {
        failed.incrementAndGet();
    }

    public long getProcessed() {
        return processed.get();
    }

    public long getSent() {
        return sent.get();
    }

    public long getFailed() {
        return failed.get();
    }

    public LocalDateTime getLastProcessedAt() {
        return lastProcessedAt;
    }
}
