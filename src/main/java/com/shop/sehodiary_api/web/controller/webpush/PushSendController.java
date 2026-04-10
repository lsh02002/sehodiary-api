package com.shop.sehodiary_api.web.controller.webpush;

import com.shop.sehodiary_api.service.webpush.WebPushService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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