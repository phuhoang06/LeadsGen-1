package com.mm.image_aws.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;


@Service
public class RateLimitingService {

    private final StringRedisTemplate redisTemplate;
    private static final String RATE_LIMIT_PREFIX = "rate_limit:upload:";
    private static final int PERMIT_COUNT = 1; // Number of permits allowed
    private static final Duration TIME_WINDOW = Duration.ofSeconds(1); // Time window for the permits
    private static final Logger logger = LoggerFactory.getLogger(RateLimitingService.class);

    @Autowired
    public RateLimitingService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }


    public boolean tryAcquire(String userId) {
        try {
            String key = RATE_LIMIT_PREFIX + userId;
            ValueOperations<String, String> ops = redisTemplate.opsForValue();


            Long currentCount = ops.increment(key);


            if (currentCount != null && currentCount == 1) {
                redisTemplate.expire(key, TIME_WINDOW);
                logger.debug("Rate limit key created for user: {} with expiration: {} seconds", userId, TIME_WINDOW.getSeconds());
            }


            boolean allowed = currentCount != null && currentCount <= PERMIT_COUNT;
            
            if (allowed) {
                logger.debug("Rate limit check passed for user: {} (count: {}/{})", userId, currentCount, PERMIT_COUNT);
            } else {
                logger.warn("Rate limit exceeded for user: {} (count: {}/{})", userId, currentCount, PERMIT_COUNT);
            }
            
            return allowed;
        } catch (Exception e) {
            logger.warn("Redis connection failed, allowing request without rate limiting for user: {} - Error: {}", userId, e.getMessage());
            return true; // Allow request when Redis is not available
        }
    }


    public Long getRemainingTime(String userId) {
        try {
            String key = RATE_LIMIT_PREFIX + userId;
            return redisTemplate.getExpire(key);
        } catch (Exception e) {
            logger.warn("Failed to get remaining time for user: {} - Error: {}", userId, e.getMessage());
            return null;
        }
    }
}
