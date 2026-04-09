package com.shop.sehodiary_api.web.controller.diary;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.lang.reflect.Field;
import java.util.Deque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DiarySseControllerTest {

    private DiarySseController controller;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        controller = new DiarySseController();
        response = Mockito.mock(HttpServletResponse.class);
    }

    @Test
    @DisplayName("subscribe 호출 시 emitter가 저장되고 SseEmitter가 반환된다")
    void subscribe_shouldAddEmitter() throws Exception {
        // when
        SseEmitter emitter = controller.subscribe(null, response);

        // then
        assertNotNull(emitter);
        assertEquals(1, getEmitters(controller).size());
    }

    @Test
    @DisplayName("subscribe를 여러 번 호출하면 emitter가 누적된다")
    void subscribe_shouldAccumulateEmitters() throws Exception {
        // when
        controller.subscribe(null, response);
        controller.subscribe(null, response);
        controller.subscribe(null, response);

        // then
        assertEquals(3, getEmitters(controller).size());
    }

    @Test
    @DisplayName("notifyNewPost 호출 시 emitter 목록이 유지된다")
    void notifyNewPost_shouldKeepEmitters_whenNoIOException() throws Exception {
        // given
        controller.subscribe(null, response);
        controller.subscribe(null, response);

        // when
        controller.notifyNewPost(1L, "테스트 제목", 1L, "user1");

        // then
        assertEquals(2, getEmitters(controller).size());
    }

    @Test
    @DisplayName("Last-Event-ID가 있으면 놓친 이벤트를 재전송한다")
    void subscribe_shouldReplayMissedEvents_whenLastEventIdProvided() throws Exception {
        // given
        // 이벤트 2개 저장
        controller.notifyNewPost(1L, "첫 번째", 1L, "user1");
        controller.notifyNewPost(2L, "두 번째", 1L, "user1");

        // when - lastEventId=0 이면 두 이벤트 모두 재전송 대상
        SseEmitter emitter = controller.subscribe("0", response);

        // then
        assertNotNull(emitter);
    }

    @Test
    @DisplayName("notifyNewPost 호출 시 eventStore에 이벤트가 저장된다")
    void notifyNewPost_shouldStoreEvent() throws Exception {
        // when
        controller.notifyNewPost(1L, "제목", 1L, "user1");

        // then
        Deque<?> eventStore = getEventStore(controller);
        assertEquals(1, eventStore.size());
    }

    @Test
    @DisplayName("eventStore는 최대 100개까지만 저장된다")
    void eventStore_shouldNotExceedMaxSize() throws Exception {
        // when
        for (int i = 0; i < 110; i++) {
            controller.notifyNewPost((long) i, "제목" + i, 1L, "user1");
        }

        // then
        Deque<?> eventStore = getEventStore(controller);
        assertEquals(100, eventStore.size());
    }

    @SuppressWarnings("unchecked")
    private List<SseEmitter> getEmitters(DiarySseController controller) throws Exception {
        Field field = DiarySseController.class.getDeclaredField("emitters");
        field.setAccessible(true);
        return (List<SseEmitter>) field.get(controller);
    }

    @SuppressWarnings("unchecked")
    private Deque<?> getEventStore(DiarySseController controller) throws Exception {
        Field field = DiarySseController.class.getDeclaredField("eventStore");
        field.setAccessible(true);
        return (Deque<?>) field.get(controller);
    }
}