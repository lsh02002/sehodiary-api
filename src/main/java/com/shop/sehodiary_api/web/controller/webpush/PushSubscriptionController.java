package com.shop.sehodiary_api.web.controller.webpush;

import com.shop.sehodiary_api.repository.webpush.PushSubscription;
import com.shop.sehodiary_api.repository.webpush.PushSubscriptionRepository;
import com.shop.sehodiary_api.web.dto.webpush.PushSubscriptionRequest;
import com.shop.sehodiary_api.web.dto.webpush.WebPushProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushSubscriptionController {

    private final PushSubscriptionRepository repository;
    private final WebPushProperties properties;

    @GetMapping("/public-key")
    public Map<String, String> publicKey() {
        return Map.of("publicKey", properties.getPublicKey());
    }

    @PostMapping("/subscribe")
    public ResponseEntity<Void> subscribe(@RequestBody PushSubscriptionRequest request) {
        PushSubscription subscription = repository.findByEndpoint(request.getEndpoint())
                .orElseGet(PushSubscription::new);

        subscription.setUserId(request.getUserId());
        subscription.setEndpoint(request.getEndpoint());
        subscription.setP256dh(request.getKeys().getP256dh());
        subscription.setAuth(request.getKeys().getAuth());

        repository.save(subscription);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/unsubscribe")
    public ResponseEntity<Void> unsubscribe(@RequestParam String endpoint) {
        repository.findByEndpoint(endpoint).ifPresent(repository::delete);
        return ResponseEntity.ok().build();
    }
}