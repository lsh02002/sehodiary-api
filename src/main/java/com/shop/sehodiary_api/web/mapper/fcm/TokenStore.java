package com.shop.sehodiary_api.web.mapper.fcm;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenStore {

    private final Map<String, String> userTokenMap = new ConcurrentHashMap<>();

    public void save(String userId, String token) {
        userTokenMap.put(userId, token);
    }

    public String findByUserId(String userId) {
        return userTokenMap.get(userId);
    }
}
