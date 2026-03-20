package com.shop.sehodiary_api.web.controller.like;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.sehodiary_api.repository.user.userDetails.CustomUserDetails;
import com.shop.sehodiary_api.service.like.LikeService;
import com.shop.sehodiary_api.web.dto.diary.DiaryResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LikeController.class)
@AutoConfigureMockMvc
class LikeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LikeService likeService;

    private static final Long USER_ID = 1L;

    private CustomUserDetails createCustomUserDetails(Long userId) {
        CustomUserDetails userDetails = Mockito.mock(CustomUserDetails.class);
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
    @DisplayName("GET /like/nicknames/{diaryId}")
    @WithMockUser(roles = "USER")
    class GetLikingNicknamesByDiaryTest {

        @Test
        @DisplayName("좋아요 누른 닉네임 목록 조회 성공")
        void getLikingNicknamesByDiary_success() throws Exception {
            Long diaryId = 10L;
            List<String> nicknames = List.of("철수", "영희", "민수");

            given(likeService.getLikingNicknamesByDiary(diaryId)).willReturn(nicknames);

            mockMvc.perform(get("/like/nicknames/{diaryId}", diaryId).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(3))
                    .andExpect(jsonPath("$[0]").value("철수"))
                    .andExpect(jsonPath("$[1]").value("영희"))
                    .andExpect(jsonPath("$[2]").value("민수"));

            then(likeService).should().getLikingNicknamesByDiary(diaryId);
        }
    }

    @Nested
    @DisplayName("GET /like/isLiked/{id}")
    class IsLikedTest {

        @Test
        @DisplayName("좋아요 여부 조회 성공 - true")
        void isLiked_true() throws Exception {
            Long diaryId = 10L;

            given(likeService.isLiked(USER_ID, diaryId)).willReturn(true);

            mockMvc.perform(get("/like/isLiked/{id}", diaryId)
                            .with(authentication(createAuthentication())))
                    .andExpect(status().isOk())
                    .andExpect(content().string("true"));

            then(likeService).should().isLiked(USER_ID, diaryId);
        }

        @Test
        @DisplayName("좋아요 여부 조회 성공 - false")
        void isLiked_false() throws Exception {
            Long diaryId = 11L;

            given(likeService.isLiked(USER_ID, diaryId)).willReturn(false);

            mockMvc.perform(get("/like/isLiked/{id}", diaryId)
                            .with(authentication(createAuthentication())))
                    .andExpect(status().isOk())
                    .andExpect(content().string("false"));

            then(likeService).should().isLiked(USER_ID, diaryId);
        }
    }

    @Nested
    @DisplayName("GET /like/user")
    class GetMyLikedDiariesTest {

        @Test
        @DisplayName("내가 좋아요한 일기 목록 조회 성공")
        void getMyLikedDiaries_success() throws Exception {
            DiaryResponse diary1 = Mockito.mock(DiaryResponse.class);
            DiaryResponse diary2 = Mockito.mock(DiaryResponse.class);

            given(likeService.getMyLikedDiaries(USER_ID)).willReturn(List.of(diary1, diary2));

            mockMvc.perform(get("/like/user")
                            .with(authentication(createAuthentication())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));

            then(likeService).should().getMyLikedDiaries(USER_ID);
        }
    }

    @Nested
    @DisplayName("POST /like/{id}")
    class InsertTest {

        @Test
        @DisplayName("좋아요 추가 성공 - true 반환")
        void insert_true() throws Exception {
            Long diaryId = 10L;

            given(likeService.insert(USER_ID, diaryId)).willReturn(true);

            mockMvc.perform(post("/like/{id}", diaryId)
                            .with(authentication(createAuthentication())).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("true"));

            then(likeService).should().insert(USER_ID, diaryId);
        }

        @Test
        @DisplayName("좋아요 추가 성공 - false 반환")
        void insert_false() throws Exception {
            Long diaryId = 10L;

            given(likeService.insert(USER_ID, diaryId)).willReturn(false);

            mockMvc.perform(post("/like/{id}", diaryId)
                            .with(authentication(createAuthentication())).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("false"));

            then(likeService).should().insert(USER_ID, diaryId);
        }
    }

    @Nested
    @DisplayName("DELETE /like/{id}")
    class DeleteTest {

        @Test
        @DisplayName("좋아요 삭제 성공 - true 반환")
        void delete_true() throws Exception {
            Long diaryId = 10L;

            given(likeService.delete(USER_ID, diaryId)).willReturn(true);

            mockMvc.perform(delete("/like/{id}", diaryId)
                            .with(authentication(createAuthentication())).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("true"));

            then(likeService).should().delete(USER_ID, diaryId);
        }

        @Test
        @DisplayName("좋아요 삭제 성공 - false 반환")
        void delete_false() throws Exception {
            Long diaryId = 10L;

            given(likeService.delete(USER_ID, diaryId)).willReturn(false);

            mockMvc.perform(delete("/like/{id}", diaryId)
                            .with(authentication(createAuthentication())).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("false"));

            then(likeService).should().delete(USER_ID, diaryId);
        }
    }
}