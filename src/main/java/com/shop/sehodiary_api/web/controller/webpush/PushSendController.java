package com.shop.sehodiary_api.web.controller.webpush;

import com.shop.sehodiary_api.service.webpush.WebPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushSendController {

    private final WebPushService webPushService;

    @PostMapping("/broadcast")
    public ResponseEntity<Void> broadcast(@RequestBody Map<String, String> body) {

        webPushService.broadcast(
                body.getOrDefault("title", "알림"),
                body.getOrDefault("message", "새 메시지가 도착했습니다."),
                body.getOrDefault("url", "/")
        );
        return ResponseEntity.ok().build();
    }
}