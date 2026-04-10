package com.shop.sehodiary_api.config.webpush;

import com.shop.sehodiary_api.web.dto.webpush.WebPushProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(WebPushProperties.class)
public class WebPushConfig {
}
