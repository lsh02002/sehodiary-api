package com.shop.sehodiary_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

import java.util.TimeZone;

@SpringBootApplication
@EnableRedisRepositories(
        basePackages = "com.shop.sehodiary_api.config.redis.refreshToken"
)
@EnableJpaRepositories(
        basePackages = "com.shop.sehodiary_api.repository"
)
@EntityScan(
        basePackages = "com.shop.sehodiary_api.repository"
)
public class SehoDiaryApiApplication {

    public static void main(String[] args)
    {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        SpringApplication.run(SehoDiaryApiApplication.class, args);
    }
}
