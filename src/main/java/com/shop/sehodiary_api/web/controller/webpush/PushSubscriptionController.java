package com.shop.sehodiary_api.web.controller.webpush;

import com.shop.sehodiary_api.repository.user.userDetails.CustomUserDetails;
import com.shop.sehodiary_api.repository.webpush.PushSubscription;
import com.shop.sehodiary_api.repository.webpush.PushSubscriptionRepository;
import com.shop.sehodiary_api.web.dto.webpush.PushSubscriptionRequest;
import com.shop.sehodiary_api.web.dto.webpush.WebPushProperties;
import com.sun.jdi.LongValue;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;

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
    public ResponseEntity<Void> subscribe(
            @RequestBody PushSubscriptionRequest request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
            ) {
        PushSubscription subscription = repository.findByEndpoint(request.getEndpoint())
                .orElseGet(PushSubscription::new);

        Long userId = (customUserDetails != null) ? customUserDetails.getId() : null;

        subscription.setUserId(userId);
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