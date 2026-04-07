package com.shibashis.coldmailer.v1.queue;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
public class EmailJobQueueService {

    private static final Logger logger = LoggerFactory.getLogger(EmailJobQueueService.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisConnectionFactory redisConnectionFactory;
    private final String queueName;

    public EmailJobQueueService(RedisTemplate<String, Object> redisTemplate,
                                RedisConnectionFactory redisConnectionFactory,
                                @Value("${app.queue.email-jobs-key:email:jobs}") String queueName) {
        this.redisTemplate = redisTemplate;
        this.redisConnectionFactory = redisConnectionFactory;
        this.queueName = queueName;
    }

    public void push(EmailJobPayload payload) {
        redisTemplate.opsForList().leftPush(queueName, payload);
        Long size = redisTemplate.opsForList().size(queueName);
        logger.info("queue_push queue={} campaignId={} campaignContactId={} sizeAfterPush={}",
                queueName, payload.getCampaignId(), payload.getCampaignContactId(), size);
    }

    @SuppressWarnings("unchecked")
    public Optional<EmailJobPayload> blockingPop(Duration timeout) {
        Object value = redisTemplate.opsForList().rightPop(queueName, timeout);
        if (value == null) {
            return Optional.empty();
        }
        logger.info("queue_pop queue={} payloadType={}", queueName, value.getClass().getName());
        return Optional.of((EmailJobPayload) value);
    }

    public long size() {
        Long size = redisTemplate.opsForList().size(queueName);
        return size == null ? 0L : size;
    }

    public void touchConnection() {
        RedisConnection connection = redisConnectionFactory.getConnection();
        connection.ping().getBytes(StandardCharsets.UTF_8);
        connection.close();
    }
}
