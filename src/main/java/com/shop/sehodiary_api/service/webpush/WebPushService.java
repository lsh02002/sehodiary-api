package com.shop.sehodiary_api.service.webpush;

import com.shop.sehodiary_api.repository.webpush.PushSubscription;
import com.shop.sehodiary_api.repository.webpush.PushSubscriptionRepository;
import com.shop.sehodiary_api.web.dto.webpush.WebPushProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;

import java.security.Security;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WebPushService {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final WebPushProperties properties;

    @PostConstruct
    public void init() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public void broadcast(String title, String body, String url) {
        List<PushSubscription> subscriptions = pushSubscriptionRepository.findAll();

        for (PushSubscription sub : subscriptions) {
            try {
                send(sub, title, body, url);
            } catch (Exception e) {
                // 죽은 구독 제거
//                pushSubscriptionRepository.delete(sub);
                e.printStackTrace();
            }
        }
    }

    private void send(PushSubscription sub, String title, String body, String url) throws Exception {

        PushService pushService = new PushService(
                properties.getPublicKey(),
                properties.getPrivateKey(),
                properties.getSubject()
        );

        String payload = """
            {
              "title": "%s",
              "body": "%s",
              "url": "%s",
              "userId": "%s"
            }
            """.formatted(
                escape(title),
                escape(body),
                escape(url),
                escape(sub.getUserId().toString())
        );

        Subscription subscription = new Subscription(
                sub.getEndpoint(),
                new Subscription.Keys(sub.getP256dh(), sub.getAuth())
        );

        Notification notification = new Notification(subscription, payload);

        pushService.send(notification);
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\"", "\\\"");
    }
}