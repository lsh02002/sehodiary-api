package com.shop.sehodiary_api.web.controller.diary;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/sse")
public class DiarySseController {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private volatile boolean hasNewDiary = false;

    @GetMapping(value = "/posts", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data(Map.of("hasNewDiary", hasNewDiary)));
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        return emitter;
    }

    @GetMapping("/posts/has-new")
    public Map<String, Boolean> hasNew() {
        return Map.of("hasNewDiary", hasNewDiary);
    }

    @PatchMapping("/posts/has-new/read")
    public void markAsRead() {
        hasNewDiary = false;
    }

    public void notifyNewPost(Long postId, String title, Long userId, String nickname) {
        hasNewDiary = true;

        List<SseEmitter> deadEmitters = new ArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("new-post")
                        .data(Map.of(
                                "postId", postId,
                                "userId", userId,
                                "title", title,
                                "nickname", nickname,
                                "hasNewDiary", true
                        )));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        }

        emitters.removeAll(deadEmitters);
    }
}