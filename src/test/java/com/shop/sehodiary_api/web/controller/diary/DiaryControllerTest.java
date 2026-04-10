package com.shop.sehodiary_api.web.controller.diary;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.sehodiary_api.repository.common.Visibility;
import com.shop.sehodiary_api.repository.user.userDetails.CustomUserDetails;
import com.shop.sehodiary_api.service.diary.DiaryService;
import com.shop.sehodiary_api.web.dto.diary.DiaryRequest;
import com.shop.sehodiary_api.web.dto.diary.DiaryResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DiaryController.class)
@ActiveProfiles("test")
class DiaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DiaryService diaryService;

    @Autowired
    private ObjectMapper objectMapper;

    private CustomUserDetails createCustomUserDetails(Long id) {
        // 프로젝트의 CustomUserDetails 생성자에 맞게 수정
        return new CustomUserDetails(id, "test@test.com", "password", "test", List.of());
    }

    private DiaryResponse createDiaryResponse(Long id, String title) {
        // 실제 DiaryResponse 생성 방식(builder/constructor)에 맞게 수정
        return DiaryResponse.builder()
                .id(id)
                .title(title)
                .content("content")
                .build();
    }

    @Nested
    @DisplayName("공개 다이어리 조회")
    @WithMockUser(roles = "USER")
    class GetPublicDiariesTest {

        @Test
        @DisplayName("성공")
        void getDiariesByPublic_success() throws Exception {
            List<DiaryResponse> content = List.of(
                    createDiaryResponse(1L, "public diary 1"),
                    createDiaryResponse(2L, "public diary 2")
            );

            Page<DiaryResponse> response = new PageImpl<>(content);

            given(diaryService.getDiariesByPublic(any(), any())).willReturn(response);

            mockMvc.perform(get("/diary/public").with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.content[0].title").value("public diary 1"))
                    .andExpect(jsonPath("$.content[1].id").value(2))
                    .andExpect(jsonPath("$.content[1].title").value("public diary 2"));
        }
    }

    @Nested
    @DisplayName("친구 다이어리 조회")
    @WithMockUser(roles = "USER")
    class GetFriendsDiariesTest {

        @Test
        @DisplayName("성공")
        void getDiariesByFriends_success() throws Exception {
            List<DiaryResponse> content = List.of(
                    createDiaryResponse(3L, "friend diary 1")
            );

            Page<DiaryResponse> response = new PageImpl<>(content);

            given(diaryService.getDiariesByFriends(any(), any())).willReturn(response);

            mockMvc.perform(get("/diary/friends").with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(3))
                    .andExpect(jsonPath("$.content[0].title").value("friend diary 1"));
        }
    }

    @Nested
    @DisplayName("내 다이어리 조회")
    @WithMockUser(roles = "USER")
    class GetUserDiariesTest {

        @Test
        @DisplayName("성공")
        void getDiariesByUser_success() throws Exception {
            Long userId = 1L;
            CustomUserDetails userDetails = createCustomUserDetails(userId);

            List<DiaryResponse> content = List.of(
                    createDiaryResponse(10L, "my diary 1")
            );

            Page<DiaryResponse> response = new PageImpl<>(content);

            given(diaryService.getDiariesByUser(eq(userId), eq(userId), any(Pageable.class))).willReturn(response);

            mockMvc.perform(get("/diary/user")
                            .with(authentication(
                                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                            userDetails, null, userDetails.getAuthorities()
                                    )
                            )).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(10))
                    .andExpect(jsonPath("$.content[0].title").value("my diary 1"));
        }
    }

    @Nested
    @DisplayName("팔로우한 사람의 다이어리 조회")
    @WithMockUser(roles = "USER")
    class GetDiariesPublicAndFriendsByUserTest {
        @Test
        @DisplayName("공개 및 친구 공개 다이어리 목록 조회 성공")
        void getDiariesPublicAndFriendsByUser_success() throws Exception {
            // given
            Long loginUserId = 1L;
            Long targetUserId = 2L;

            CustomUserDetails customUserDetails = createCustomUserDetails(loginUserId);

            DiaryResponse diary1 = createDiaryResponse(101L, "첫 번째 일기");
            DiaryResponse diary2 = createDiaryResponse(102L, "두 번째 일기");

            List<DiaryResponse> content = List.of(diary1, diary2);

            Page<DiaryResponse> response = new PageImpl<>(content);

            given(diaryService.getDiariesPublicAndFriendsByUser(eq(loginUserId), eq(targetUserId), any()))
                    .willReturn(response);

            // when & then
            mockMvc.perform(get("/diary/{targetUserId}/user", targetUserId)
                            .with(authentication(
                                    new UsernamePasswordAuthenticationToken(
                                            customUserDetails,
                                            null,
                                            List.of(new SimpleGrantedAuthority("ROLE_USER"))
                                    )
                            ))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(101L))
                    .andExpect(jsonPath("$.content[0].title").value("첫 번째 일기"))
                    .andExpect(jsonPath("$.content[1].id").value(102L))
                    .andExpect(jsonPath("$.content[1].title").value("두 번째 일기"));
        }

        @Test
        @DisplayName("조회 결과가 없으면 빈 리스트 반환")
        void getDiariesPublicAndFriendsByUser_emptyList() throws Exception {
            // given
            Long loginUserId = 1L;
            Long targetUserId = 99L;

            CustomUserDetails customUserDetails = createCustomUserDetails(loginUserId);

            given(diaryService.getDiariesPublicAndFriendsByUser(eq(loginUserId), eq(targetUserId), any()))
                    .willReturn(Page.empty());

            // when & then
            mockMvc.perform(get("/diary/{targetUserId}/user", targetUserId)
                            .with(authentication(
                                    new UsernamePasswordAuthenticationToken(
                                            customUserDetails,
                                            null,
                                            List.of(new SimpleGrantedAuthority("ROLE_USER"))
                                    )
                            ))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }

        private CustomUserDetails createCustomUserDetails(Long userId) {
            // 실제 CustomUserDetails 생성 방식에 맞게 수정 필요
            return org.mockito.Mockito.mock(CustomUserDetails.class, invocation -> {
                if ("getId".equals(invocation.getMethod().getName())) {
                    return userId;
                }
                return null;
            });
        }
    }

    @Nested
    @DisplayName("단건 다이어리 조회")
    @WithMockUser(roles = "USER")
    class GetOneDiaryTest {

        @Test
        @DisplayName("성공")
        void getOneDiary_success() throws Exception {
            Long diaryId = 1L;
            DiaryResponse response = createDiaryResponse(diaryId, "one diary");

            given(diaryService.getOneDiary(diaryId)).willReturn(response);

            mockMvc.perform(get("/diary/one/{diaryId}", diaryId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(diaryId))
                    .andExpect(jsonPath("$.title").value("one diary"));
        }
    }

    @Nested
    @DisplayName("다이어리 생성")
    class CreateDiaryTest {

        @Test
        @DisplayName("성공 - 파일 포함")
        void createDiary_success_withFiles() throws Exception {
            Long userId = 1L;
            CustomUserDetails userDetails = createCustomUserDetails(userId);

            DiaryRequest request = new DiaryRequest("title", "content", LocalDate.now().toString(), Visibility.PUBLIC.name(), "맑음", "😊");
            DiaryResponse response = createDiaryResponse(100L, "title");

            MockMultipartFile requestPart = new MockMultipartFile(
                    "request",
                    "",
                    MediaType.APPLICATION_JSON_VALUE,
                    objectMapper.writeValueAsBytes(request)
            );

            MockMultipartFile file = new MockMultipartFile(
                    "files",
                    "test.jpg",
                    MediaType.IMAGE_JPEG_VALUE,
                    "image-content".getBytes(StandardCharsets.UTF_8)
            );

            given(diaryService.createDiary(eq(userId), any(DiaryRequest.class), anyList()))
                    .willReturn(response);

            mockMvc.perform(multipart("/diary/create")
                            .file(requestPart)
                            .file(file)
                            .with(authentication(
                                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                            userDetails, null, userDetails.getAuthorities()
                                    )
                            )).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(100))
                    .andExpect(jsonPath("$.title").value("title"));
        }

        @Test
        @DisplayName("성공 - 파일 없음")
        void createDiary_success_withoutFiles() throws Exception {
            Long userId = 1L;
            CustomUserDetails userDetails = createCustomUserDetails(userId);

            DiaryRequest request = new DiaryRequest("title", "content", LocalDate.now().toString(), Visibility.PUBLIC.name(), "맑음", "😊");
            DiaryResponse response = createDiaryResponse(101L, "title");

            MockMultipartFile requestPart = new MockMultipartFile(
                    "request",
                    "",
                    MediaType.APPLICATION_JSON_VALUE,
                    objectMapper.writeValueAsBytes(request)
            );

            given(diaryService.createDiary(eq(userId), any(DiaryRequest.class), ArgumentMatchers.isNull()))
                    .willReturn(response);

            mockMvc.perform(multipart("/diary/create")
                            .file(requestPart)
                            .with(authentication(
                                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                            userDetails, null, userDetails.getAuthorities()
                                    )
                            )).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(101))
                    .andExpect(jsonPath("$.title").value("title"));
        }
    }

    @Nested
    @DisplayName("다이어리 수정")
    class EditDiaryTest {

        @Test
        @DisplayName("성공")
        void editDiary_success() throws Exception {
            Long userId = 1L;
            Long diaryId = 200L;
            CustomUserDetails userDetails = createCustomUserDetails(userId);

            DiaryRequest request = new DiaryRequest("title", "content", LocalDate.now().toString(), Visibility.PUBLIC.name(), "맑음", "😊");
            DiaryResponse response = createDiaryResponse(diaryId, "edit title");

            MockMultipartFile requestPart = new MockMultipartFile(
                    "request",
                    "",
                    MediaType.APPLICATION_JSON_VALUE,
                    objectMapper.writeValueAsBytes(request)
            );

            MockMultipartFile file = new MockMultipartFile(
                    "files",
                    "edit.jpg",
                    MediaType.IMAGE_JPEG_VALUE,
                    "edit-image".getBytes(StandardCharsets.UTF_8)
            );

            given(diaryService.editDiary(eq(userId), eq(diaryId), any(DiaryRequest.class), anyList()))
                    .willReturn(response);

            mockMvc.perform(multipart("/diary/edit/{diaryId}", diaryId)
                            .file(requestPart)
                            .file(file)
                            .with(authentication(
                                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                            userDetails, null, userDetails.getAuthorities()
                                    )
                            ))
                            .with(request0 -> {
                                request0.setMethod("PUT");
                                return request0;
                            }).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(diaryId))
                    .andExpect(jsonPath("$.title").value("edit title"));
        }
    }

    @Nested
    @DisplayName("다이어리 삭제")
    class DeleteDiaryTest {

        @Test
        @DisplayName("성공")
        void deleteDiary_success() throws Exception {
            Long userId = 1L;
            Long diaryId = 300L;
            CustomUserDetails userDetails = createCustomUserDetails(userId);

            willDoNothing().given(diaryService).deleteDiary(userId, diaryId);

            mockMvc.perform(delete("/diary/{diaryId}", diaryId)
                            .with(authentication(
                                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                                            userDetails, null, userDetails.getAuthorities()
                                    )
                            )).with(csrf()))
                    .andExpect(status().isOk());
        }
    }
}