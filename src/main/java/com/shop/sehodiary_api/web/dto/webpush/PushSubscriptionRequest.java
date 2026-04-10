package com.shop.sehodiary_api.web.dto.webpush;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PushSubscriptionRequest {
    private Long userId;
    private String endpoint;
    private Keys keys;

    @Getter @Setter
    public static class Keys {
        private String p256dh;
        private String auth;
    }
}
