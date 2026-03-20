package com.shop.sehodiary_api.web.controller.emotion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.sehodiary_api.repository.user.userDetails.CustomUserDetails;
import com.shop.sehodiary_api.service.emotion.EmotionService;
import com.shop.sehodiary_api.web.dto.emotion.EmotionRequest;
import com.shop.sehodiary_api.web.dto.emotion.EmotionResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmotionController.class)
@AutoConfigureMockMvc
class EmotionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmotionService emotionService;

    private static final Long USER_ID = 1L;

    /**
     * CustomUserDetails 생성 방식이 프로젝트마다 달라서
     * 아래 메서드는 네 프로젝트 생성자/빌더에 맞게 수정하면 됨.
     */
    private CustomUserDetails createCustomUserDetails(Long userId) {
        // 예시 1) 생성자가 있는 경우
        // return new CustomUserDetails(userId, "test@test.com", "password", List.of(new SimpleGrantedAuthority("ROLE_USER")));

        // 예시 2) mock으로 처리
        CustomUserDetails userDetails = org.mockito.Mockito.mock(CustomUserDetails.class);
        given(userDetails.getId()).willReturn(userId);
        return userDetails;
    }

    private UsernamePasswordAuthenticationToken createAuthentication() {
        CustomUserDetails userDetails = createCustomUserDetails(USER_ID);
        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Nested
    @DisplayName("POST /emotion/insertEmojisIntoNewDB")
    class InsertEmojisIntoNewDBTest {

        @Test
        @DisplayName("성공")
        void insertEmojisIntoNewDB_success() throws Exception {
            willDoNothing().given(emotionService).insertEmojisIntoNewDB(USER_ID);

            mockMvc.perform(post("/emotion/insertEmojisIntoNewDB")
                            .with(authentication(createAuthentication())).with(csrf()))
                    .andExpect(status().isOk());

        }
    }

    @Nested
    @DisplayName("GET /emotion/all")
    @WithMockUser(roles = "USER")
    class GetAllEmotionsTest {

        @Test
        @DisplayName("전체 감정 조회 성공")
        void getAllEmotions_success() throws Exception {
            EmotionResponse response1 = new EmotionResponse(1L, "행복", "😊", LocalDateTime.now().toString(), LocalDateTime.now().toString());
            EmotionResponse response2 = new EmotionResponse(2L, "슬픔", "😢", LocalDateTime.now().toString(), LocalDateTime.now().toString());

            given(emotionService.getAllEmotions()).willReturn(List.of(response1, response2));

            mockMvc.perform(get("/emotion/all").with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").value(1L))
                    .andExpect(jsonPath("$[0].name").value("행복"))
                    .andExpect(jsonPath("$[0].emoji").value("😊"))
                    .andExpect(jsonPath("$[1].id").value(2L))
                    .andExpect(jsonPath("$[1].name").value("슬픔"))
                    .andExpect(jsonPath("$[1].emoji").value("😢"));
        }
    }

    @Nested
    @DisplayName("POST /emotion/create")
    class CreateEmotionTest {

        @Test
        @DisplayName("감정 생성 성공")
        void createEmotion_success() throws Exception {
            EmotionRequest request = new EmotionRequest("행복", "😊");
            EmotionResponse response = new EmotionResponse(1L, "행복", "😊", LocalDateTime.now().toString(), LocalDateTime.now().toString());

            given(emotionService.createEmotion(eq(USER_ID), any(EmotionRequest.class)))
                    .willReturn(response);

            mockMvc.perform(post("/emotion/create")
                            .with(authentication(createAuthentication()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.name").value("행복"))
                    .andExpect(jsonPath("$.emoji").value("😊"));
        }
    }

    @Nested
    @DisplayName("POST /emotion/edit/{emotionId}")
    class EditEmotionTest {

        @Test
        @DisplayName("감정 수정 성공")
        void editEmotion_success() throws Exception {
            Long emotionId = 10L;
            EmotionRequest request = new EmotionRequest("기쁨", "😄");
            EmotionResponse response = new EmotionResponse(emotionId, "기쁨", "😄", LocalDateTime.now().toString(), LocalDateTime.now().toString());

            given(emotionService.editEmotion(eq(USER_ID), eq(emotionId), any(EmotionRequest.class)))
                    .willReturn(response);

            mockMvc.perform(post("/emotion/edit/{emotionId}", emotionId)
                            .with(authentication(createAuthentication()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(emotionId))
                    .andExpect(jsonPath("$.name").value("기쁨"))
                    .andExpect(jsonPath("$.emoji").value("😄"));
        }
    }

    @Nested
    @DisplayName("DELETE /emotion/{emotionId}")
    class DeleteEmotionTest {

        @Test
        @DisplayName("감정 삭제 성공")
        void deleteEmotion_success() throws Exception {
            Long emotionId = 10L;
            willDoNothing().given(emotionService).deleteEmotion(USER_ID, emotionId);

            mockMvc.perform(delete("/emotion/{emotionId}", emotionId)
                            .with(authentication(createAuthentication())).with(csrf()))
                    .andExpect(status().isOk());
        }
    }
}