package com.shop.sehodiary_api.config.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisUtil {
    private final RedisTemplate<String, Object> redisBlackListTemplate;

    public void setBlackList(String key, Object o, int minutes) {
        redisBlackListTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(o.getClass()));
        redisBlackListTemplate.opsForValue().set(key, o, minutes, TimeUnit.MINUTES);
    }

    public boolean hasKeyBlackList(String key) {
        return Boolean.TRUE.equals(redisBlackListTemplate.hasKey(key));
    }
}