package com.shop.sehodiary_api.web.dto.webpush;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "webpush")
@Getter
@Setter
public class WebPushProperties {
    private String publicKey;
    private String privateKey;
    private String subject;
}
