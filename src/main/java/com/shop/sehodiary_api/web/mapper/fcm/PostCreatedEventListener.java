package com.shop.sehodiary_api.web.mapper.fcm;

import com.shop.sehodiary_api.service.fcm.FcmService;
import com.shop.sehodiary_api.web.dto.fcm.PostCreatedEvent;
import com.shop.sehodiary_api.web.dto.fcm.PushSendRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

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
    public void handleAfterCommit(PostCreatedEvent event) {
        log.info("AFTER_COMMIT invoked: {}", event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void handleAfterRollback(PostCreatedEvent event) {
        log.info("AFTER_ROLLBACK invoked: {}", event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMPLETION)
    public void handleAfterCompletion(PostCreatedEvent event) {
        log.info("AFTER_COMPLETION invoked: {}", event);
    }

//    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
//    public void handle(PostCreatedEvent event) {
//        try {
//            // 예시: 작성자 본인에게 보내기
//            String token = tokenStore.findByUserId(event.authorId());
//
//            if (token == null || token.isBlank()) {
//                return;
//            }
//
//            fcmService.sendToToken(new PushSendRequest(
//                    token,
//                    "게시글 등록 완료",
//                    event.title(),
//                    Map.of(
//                            "type", "POST_CREATED",
//                            "postId", String.valueOf(event.postId()),
//                            "screen", "post_detail"
//                    )
//            ));
//            log.info("FCM send successfully");
//        } catch (Exception e) {
//            // 실무에서는 로깅/재시도 큐 처리 권장
//            log.error("FCM send failed: {}", e.getMessage());
//        }
//    }
}
