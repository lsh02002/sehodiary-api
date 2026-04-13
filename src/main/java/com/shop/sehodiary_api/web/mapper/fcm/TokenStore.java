package com.shop.sehodiary_api.web.mapper.fcm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class TokenStore {

    // deviceId 기준 저장
    private final Map<String, TokenInfo> deviceTokenMap = new ConcurrentHashMap<>();

    public void save(String token, String deviceId, Long userId) {
        deviceTokenMap.put(deviceId, new TokenInfo(token, userId));
    }

    public String findByUserId(Long userId) {
        return deviceTokenMap.values().stream()
                .filter(info -> userId != null && userId.equals(info.userId()))
                .map(TokenInfo::token)
                .findFirst()
                .orElse(null);
    }

    public List<String> findAllByUserId(Long userId) {
        return deviceTokenMap.values().stream()
                .filter(info -> userId != null && userId.equals(info.userId()))
                .map(TokenInfo::token)
                .toList();
    }

    public String findByDeviceId(String deviceId) {
        TokenInfo info = deviceTokenMap.get(deviceId);
        return info != null ? info.token() : null;
    }

    record TokenInfo(String token, Long userId) {}
}