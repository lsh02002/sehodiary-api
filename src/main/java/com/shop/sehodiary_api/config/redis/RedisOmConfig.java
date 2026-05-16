package com.shop.sehodiary_api.config.redis;

import com.redis.om.spring.annotations.EnableRedisDocumentRepositories;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
@EnableRedisDocumentRepositories(
        basePackages = "com.shop.sehodiary_api.config.redis.diarysearch"
)
public class RedisOmConfig {
}