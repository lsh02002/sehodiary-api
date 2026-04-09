package com.shop.sehodiary_api.web.controller.diary;

import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/sse")
public class DiarySseController {

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    // 최근 이벤트 저장 (최대 100개)
    private final Deque<SseEvent> eventStore = new ArrayDeque<>();
    private static final int MAX_STORED_EVENTS = 100;
    private final AtomicLong eventIdCounter = new AtomicLong(0);

    @Getter
    @AllArgsConstructor
    private static class SseEvent {
        private final long id;
        private final String name;
        private final Object data;
        private final Instant createdAt;
    }

    @GetMapping(value = "/posts", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId,
            HttpServletResponse response) {

        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");

        SseEmitter emitter = createEmitter();

        // 놓친 이벤트 재전송
        if (lastEventId != null) {
            replayMissedEvents(emitter, Long.parseLong(lastEventId));
        }

        return emitter;
    }

    private SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(60_000L);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("connected"));
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        return emitter;
    }

    private void replayMissedEvents(SseEmitter emitter, long lastEventId) {
        List<SseEvent> missed;

        synchronized (eventStore) {
            missed = eventStore.stream()
                    .filter(e -> e.getId() > lastEventId)
                    .toList();
        }

        for (SseEvent event : missed) {
            try {
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(event.getId()))
                        .name(event.getName())
                        .data(event.getData()));
            } catch (IOException e) {
                emitters.remove(emitter);
                return;
            }
        }
    }

    public void notifyNewPost(Long postId, String title, Long userId, String nickname) {
        long eventId = eventIdCounter.incrementAndGet();
        Map<String, Object> data = Map.of(
                "postId", postId,
                "userId", userId,
                "title", title,
                "nickname", nickname
        );

        // 이벤트 저장
        synchronized (eventStore) {
            eventStore.addLast(new SseEvent(eventId, "new-post", data, Instant.now()));
            if (eventStore.size() > MAX_STORED_EVENTS) {
                eventStore.removeFirst();
            }
        }

        // 전송
        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(eventId))
                        .name("new-post")
                        .data(data));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        }
        emitters.removeAll(deadEmitters);
    }
}