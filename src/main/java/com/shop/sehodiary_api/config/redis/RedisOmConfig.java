package com.shop.sehodiary_api.config.redis;

import com.redis.om.spring.annotations.EnableRedisDocumentRepositories;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRedisDocumentRepositories(
        basePackages = "com.shop.sehodiary_api.config.redis.redissearch"
)
public class RedisOmConfig {
}