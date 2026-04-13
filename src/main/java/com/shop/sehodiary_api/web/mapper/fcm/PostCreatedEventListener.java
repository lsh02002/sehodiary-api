package com.shop.sehodiary_api.web.mapper.fcm;

import com.shop.sehodiary_api.service.fcm.FcmService;
import com.shop.sehodiary_api.web.dto.fcm.PostCreatedEvent;
import com.shop.sehodiary_api.web.dto.fcm.PushSendRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class PostCreatedEventListener {

    private final TokenStore tokenStore;
    private final FcmService fcmService;

    public PostCreatedEventListener(TokenStore tokenStore, FcmService fcmService) {
        this.tokenStore = tokenStore;
        this.fcmService = fcmService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PostCreatedEvent event) {
        try {
            Long authorId = Long.valueOf(event.authorId());
            List<String> tokens = tokenStore.findAllByUserId(authorId);

            if (tokens == null || tokens.isEmpty()) {
                return;
            }

            for (String token : tokens) {
                if (token == null || token.isBlank()) {
                    continue;
                }

                System.out.println(token);

                fcmService.sendToToken(new PushSendRequest(
                        token,
                        "게시글 등록 완료",
                        event.title(),
                        Map.of(
                                "type", "POST_CREATED",
                                "postId", String.valueOf(event.postId()),
                                "screen", "post_detail"
                        )
                ));
            }
        } catch (Exception e) {
            log.error("FCM send failed. authorId={}, postId={}, error={}",
                    event.authorId(), event.postId(), e.getMessage(), e);
        }
    }
}
