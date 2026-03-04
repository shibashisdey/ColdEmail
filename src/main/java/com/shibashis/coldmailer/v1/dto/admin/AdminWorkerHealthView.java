package com.shibashis.coldmailer.v1.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AdminWorkerHealthView {
    private boolean redisReachable;
    private long queueDepth;
    private long processedJobs;
    private long sentJobs;
    private long failedJobs;
    private LocalDateTime lastProcessedAt;
}
