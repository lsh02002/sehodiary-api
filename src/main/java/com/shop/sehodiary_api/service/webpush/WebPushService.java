package com.shop.sehodiary_api.service.webpush;

import com.shop.sehodiary_api.repository.webpush.PushSubscription;
import com.shop.sehodiary_api.repository.webpush.PushSubscriptionRepository;
import com.shop.sehodiary_api.web.dto.webpush.WebPushProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebPushService {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final WebPushProperties properties;

    public void broadcast(String title, String body, String url) {
        List<PushSubscription> subscriptions = pushSubscriptionRepository.findAll();

        log.info("subscriptions count={}", subscriptions.size());

        for (PushSubscription sub : subscriptions) {
            try {
                log.info("sending push to endpoint={}", sub.getEndpoint());

                // 실제 web push 전송

                log.info("push sent success");
            } catch (Exception e) {
                log.error("push send failed to endpoint={}", sub.getEndpoint(), e);
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
              "url": "%s"
            }
            """.formatted(
                escape(title),
                escape(body),
                escape(url)
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