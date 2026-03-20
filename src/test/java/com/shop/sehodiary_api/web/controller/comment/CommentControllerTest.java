package com.shop.sehodiary_api.web.controller.comment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.sehodiary_api.repository.user.userDetails.CustomUserDetails;
import com.shop.sehodiary_api.service.comment.CommentService;
import com.shop.sehodiary_api.web.dto.comment.CommentRequest;
import com.shop.sehodiary_api.web.dto.comment.CommentResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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

@WebMvcTest(CommentController.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CommentService commentService;

    private CustomUserDetails createCustomUserDetails(Long id) {
        return new CustomUserDetails(
                id,
                "test@test.com",
                "password",
                "test",
                List.of()
        );
    }

    private CommentResponse createCommentResponse(Long id, String content) {
        return CommentResponse.builder()
                .commentId(id)
                .content(content)
                .build();
    }

    @Nested
    @DisplayName("다이어리별 댓글 조회")
    @WithMockUser(roles = "USER")
    class GetCommentsByDiaryIdTest {

        @Test
        @DisplayName("성공")
        void getCommentsByDiaryId_success() throws Exception {
            Long diaryId = 1L;

            List<CommentResponse> response = List.of(
                    createCommentResponse(1L, "댓글 1"),
                    createCommentResponse(2L, "댓글 2")
            );

            given(commentService.getCommentsByDiaryId(diaryId)).willReturn(response);

            mockMvc.perform(get("/comment/diary/{diaryId}", diaryId).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].commentId").value(1L))
                    .andExpect(jsonPath("$[0].content").value("댓글 1"))
                    .andExpect(jsonPath("$[1].commentId").value(2L))
                    .andExpect(jsonPath("$[1].content").value("댓글 2"));
        }
    }

    @Nested
    @DisplayName("내 댓글 조회")
    class GetCommentsByUserTest {

        @Test
        @DisplayName("성공")
        void getCommentsByUser_success() throws Exception {
            Long userId = 1L;
            CustomUserDetails customUserDetails = createCustomUserDetails(userId);

            List<CommentResponse> response = List.of(
                    createCommentResponse(10L, "내 댓글")
            );

            given(commentService.getCommentsByUser(userId)).willReturn(response);

            mockMvc.perform(get("/comment/user")
                            .with(authentication(
                                    new UsernamePasswordAuthenticationToken(
                                            customUserDetails,
                                            null,
                                            customUserDetails.getAuthorities()
                                    )
                            )).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].commentId").value(10L))
                    .andExpect(jsonPath("$[0].content").value("내 댓글"));
        }
    }

    @Nested
    @DisplayName("댓글 생성")
    class CreateCommentTest {

        @Test
        @DisplayName("성공")
        void createComment_success() throws Exception {
            Long userId = 1L;
            CustomUserDetails customUserDetails = createCustomUserDetails(userId);

            CommentRequest request = new CommentRequest(1L, "댓글 내용");
            CommentResponse response = createCommentResponse(100L, "댓글 내용");

            given(commentService.createComment(eq(userId), any(CommentRequest.class)))
                    .willReturn(response);

            mockMvc.perform(post("/comment/create")
                            .with(authentication(
                                    new UsernamePasswordAuthenticationToken(
                                            customUserDetails,
                                            null,
                                            customUserDetails.getAuthorities()
                                    )
                            ))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.commentId").value(100L))
                    .andExpect(jsonPath("$.content").value("댓글 내용"));
        }
    }

    @Nested
    @DisplayName("댓글 수정")
    class EditCommentTest {

        @Test
        @DisplayName("성공")
        void editComment_success() throws Exception {
            Long userId = 1L;
            Long commentId = 200L;
            CustomUserDetails customUserDetails = createCustomUserDetails(userId);

            CommentRequest request = new CommentRequest(1L, "수정된 댓글");
            CommentResponse response = createCommentResponse(commentId, "수정된 댓글");

            given(commentService.editComment(eq(userId), eq(commentId), any(CommentRequest.class)))
                    .willReturn(response);

            mockMvc.perform(post("/comment/{commentId}", commentId)
                            .with(authentication(
                                    new UsernamePasswordAuthenticationToken(
                                            customUserDetails,
                                            null,
                                            customUserDetails.getAuthorities()
                                    )
                            ))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.commentId").value(200L))
                    .andExpect(jsonPath("$.content").value("수정된 댓글"));
        }
    }

    @Nested
    @DisplayName("댓글 삭제")
    class DeleteCommentTest {

        @Test
        @DisplayName("성공")
        void deleteComment_success() throws Exception {
            Long userId = 1L;
            Long commentId = 300L;
            CustomUserDetails customUserDetails = createCustomUserDetails(userId);

            willDoNothing().given(commentService).deleteComment(userId, commentId);

            mockMvc.perform(delete("/comment/{commentId}", commentId)
                            .with(authentication(
                                    new UsernamePasswordAuthenticationToken(
                                            customUserDetails,
                                            null,
                                            customUserDetails.getAuthorities()
                                    )
                            )).with(csrf()))
                    .andExpect(status().isOk());
        }
    }
}