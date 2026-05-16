package com.shop.sehodiary_api.config.redis;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@Configuration
@Profile("!redis-test")
@EnableRedisRepositories(
        basePackages = "com.shop.sehodiary_api.config.redis.refreshToken"
)
public class RedisRepositoryConfig {
}
