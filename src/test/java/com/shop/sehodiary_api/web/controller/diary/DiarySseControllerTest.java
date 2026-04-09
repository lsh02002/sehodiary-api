package com.shop.sehodiary_api.web.controller.diary;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DiarySseControllerTest {

    @Test
    @DisplayName("subscribe 호출 시 emitter가 저장되고 SseEmitter가 반환된다")
    void subscribe_shouldAddEmitter() throws Exception {
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        // given
        DiarySseController controller = new DiarySseController();

        // when
        SseEmitter emitter = controller.subscribe(response);

        // then
        assertNotNull(emitter);

        List<?> emitters = getEmitters(controller);
        assertEquals(1, emitters.size());
    }

    @Test
    @DisplayName("subscribe를 여러 번 호출하면 emitter가 누적된다")
    void subscribe_shouldAccumulateEmitters() throws Exception {
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        // given
        DiarySseController controller = new DiarySseController();

        // when
        controller.subscribe(response);
        controller.subscribe(response);
        controller.subscribe(response);

        // then
        List<?> emitters = getEmitters(controller);
        assertEquals(3, emitters.size());
    }

    @Test
    @DisplayName("notifyNewPost 호출 시 emitter 목록이 유지된다")
    void notifyNewPost_shouldKeepEmitters_whenNoIOException() throws Exception {
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        // given
        DiarySseController controller = new DiarySseController();
        controller.subscribe(response);
        controller.subscribe(response);

        // when
        controller.notifyNewPost(1L, "테스트 제목", 1L, "user1");

        // then
        List<?> emitters = getEmitters(controller);
        assertEquals(2, emitters.size());
    }

    @SuppressWarnings("unchecked")
    private List<SseEmitter> getEmitters(DiarySseController controller) throws Exception {
        Field field = DiarySseController.class.getDeclaredField("emitters");
        field.setAccessible(true);
        return (List<SseEmitter>) field.get(controller);
    }
}