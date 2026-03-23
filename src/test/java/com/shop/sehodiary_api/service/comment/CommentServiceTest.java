package com.shop.sehodiary_api.service.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.util.*;

import com.shop.sehodiary_api.repository.activity.ActivityAction;
import com.shop.sehodiary_api.repository.activity.ActivityEntityType;
import com.shop.sehodiary_api.repository.activity.function.SnapshotFunc;
import com.shop.sehodiary_api.repository.comment.Comment;
import com.shop.sehodiary_api.repository.comment.CommentCacheRepository;
import com.shop.sehodiary_api.repository.comment.CommentIdRedisRepository;
import com.shop.sehodiary_api.repository.comment.CommentRepository;
import com.shop.sehodiary_api.repository.diary.Diary;
import com.shop.sehodiary_api.repository.diary.DiaryCacheRepository;
import com.shop.sehodiary_api.repository.diary.DiaryRepository;
import com.shop.sehodiary_api.repository.user.User;
import com.shop.sehodiary_api.repository.user.UserRepository;
import com.shop.sehodiary_api.service.activelog.ActivityLogService;
import com.shop.sehodiary_api.service.exceptions.BadRequestException;
import com.shop.sehodiary_api.service.exceptions.ConflictException;
import com.shop.sehodiary_api.service.exceptions.NotFoundException;
import com.shop.sehodiary_api.web.dto.comment.CommentRequest;
import com.shop.sehodiary_api.web.dto.comment.CommentResponse;
import com.shop.sehodiary_api.web.dto.diary.DiaryResponse;
import com.shop.sehodiary_api.web.mapper.comment.CommentMapper;
import com.shop.sehodiary_api.web.mapper.diary.DiaryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private DiaryRepository diaryRepository;
    @Mock private CommentRepository commentRepository;

    @Mock private DiaryMapper diaryMapper;
    @Mock private CommentMapper commentMapper;

    @Mock private DiaryCacheRepository diaryCacheRepository;
    @Mock private CommentCacheRepository commentCacheRepository;
    @Mock private CommentIdRedisRepository commentIdRedisRepository;

    @Mock private SnapshotFunc snapshotFunc;
    @Mock private ActivityLogService activityLogService;

    @InjectMocks
    private CommentService commentService;

    @Nested
    @DisplayName("getCommentsByDiaryId()")
    class GetCommentsByDiaryIdTest {

        @Test
        @DisplayName("Redis에 diaryId별 commentIds가 있고, 모두 캐시에 있으면 캐시 데이터만 반환한다")
        void returnsOnlyCachedCommentsWhenRedisIdsExistAndAllCached() {
            Long diaryId = 1L;
            List<Long> commentIds = List.of(10L, 20L);

            CommentResponse response1 = mock(CommentResponse.class);
            CommentResponse response2 = mock(CommentResponse.class);

            when(commentIdRedisRepository.findAllByDiaryId(diaryId)).thenReturn(commentIds);
            when(commentCacheRepository.getAll()).thenReturn(Map.of(
                    10L, response1,
                    20L, response2
            ));

            List<CommentResponse> result = commentService.getCommentsByDiaryId(diaryId);

            assertThat(result).containsExactly(response1, response2);

            verify(commentRepository, never()).findAllIdsByDiaryId(anyLong());
            verify(commentRepository, never()).findAllById(anyList());
            verify(commentCacheRepository, never()).put(any());
        }

        @Test
        @DisplayName("Redis에 diaryId별 commentIds가 없으면 DB에서 ids 조회 후 Redis에 저장한다")
        void loadsIdsFromDbAndSavesToRedisWhenRedisIsEmpty() {
            Long diaryId = 1L;
            List<Long> dbIds = List.of(10L, 20L);

            Comment comment1 = mock(Comment.class);
            Comment comment2 = mock(Comment.class);

            CommentResponse response1 = mock(CommentResponse.class);
            CommentResponse response2 = mock(CommentResponse.class);

            when(commentIdRedisRepository.findAllByDiaryId(diaryId)).thenReturn(List.of());
            when(commentRepository.findAllIdsByDiaryId(diaryId)).thenReturn(dbIds);
            when(commentCacheRepository.getAll()).thenReturn(Map.of());

            when(commentRepository.findAllById(dbIds)).thenReturn(List.of(comment1, comment2));
            when(commentMapper.toResponse(comment1)).thenReturn(response1);
            when(commentMapper.toResponse(comment2)).thenReturn(response2);

            List<CommentResponse> result = commentService.getCommentsByDiaryId(diaryId);

            assertThat(result).containsExactly(response1, response2);

            verify(commentRepository).findAllIdsByDiaryId(diaryId);
            verify(commentIdRedisRepository).saveAllByDiaryId(diaryId, dbIds);
            verify(commentRepository).findAllById(dbIds);
            verify(commentCacheRepository).put(response1);
            verify(commentCacheRepository).put(response2);
        }

        @Test
        @DisplayName("캐시에 없는 comment들만 DB에서 조회해서 cache에 저장 후 결과에 추가한다")
        void loadsOnlyMissingCommentsFromDb() {
            Long diaryId = 1L;
            List<Long> commentIds = List.of(10L, 20L, 30L);

            CommentResponse cachedResponse = mock(CommentResponse.class);
            CommentResponse dbResponse1 = mock(CommentResponse.class);
            CommentResponse dbResponse2 = mock(CommentResponse.class);

            Comment comment20 = mock(Comment.class);
            Comment comment30 = mock(Comment.class);

            when(commentIdRedisRepository.findAllByDiaryId(diaryId)).thenReturn(commentIds);
            when(commentCacheRepository.getAll()).thenReturn(Map.of(
                    10L, cachedResponse
            ));

            when(commentRepository.findAllById(List.of(20L, 30L)))
                    .thenReturn(List.of(comment20, comment30));
            when(commentMapper.toResponse(comment20)).thenReturn(dbResponse1);
            when(commentMapper.toResponse(comment30)).thenReturn(dbResponse2);

            List<CommentResponse> result = commentService.getCommentsByDiaryId(diaryId);

            assertThat(result).containsExactly(cachedResponse, dbResponse1, dbResponse2);

            verify(commentRepository).findAllById(List.of(20L, 30L));
            verify(commentCacheRepository).put(dbResponse1);
            verify(commentCacheRepository).put(dbResponse2);
        }
    }

    @Nested
    @DisplayName("getCommentsByUser()")
    class GetCommentsByUserTest {

        @Test
        @DisplayName("Redis에 userId별 commentIds가 있고, 모두 캐시에 있으면 캐시 데이터만 반환한다")
        void returnsOnlyCachedCommentsWhenRedisIdsExistAndAllCached() {
            Long userId = 100L;
            List<Long> commentIds = List.of(1L, 2L);

            CommentResponse response1 = mock(CommentResponse.class);
            CommentResponse response2 = mock(CommentResponse.class);

            when(commentIdRedisRepository.findAllByUserId(userId)).thenReturn(commentIds);
            when(commentCacheRepository.getAll()).thenReturn(Map.of(
                    1L, response1,
                    2L, response2
            ));

            List<CommentResponse> result = commentService.getCommentsByUser(userId);

            assertThat(result).containsExactly(response1, response2);

            verify(commentRepository, never()).findIdsByUserId(anyLong());
            verify(commentRepository, never()).findAllById(anyList());
            verify(commentCacheRepository, never()).put(any());
        }

        @Test
        @DisplayName("Redis에 userId별 commentIds가 없으면 DB에서 ids 조회 후 Redis에 저장한다")
        void loadsIdsFromDbAndSavesToRedisWhenRedisIsEmpty() {
            Long userId = 100L;
            List<Long> dbIds = List.of(1L, 2L);

            Comment comment1 = mock(Comment.class);
            Comment comment2 = mock(Comment.class);

            CommentResponse response1 = mock(CommentResponse.class);
            CommentResponse response2 = mock(CommentResponse.class);

            when(commentIdRedisRepository.findAllByUserId(userId)).thenReturn(List.of());
            when(commentRepository.findIdsByUserId(userId)).thenReturn(dbIds);
            when(commentCacheRepository.getAll()).thenReturn(Map.of());

            when(commentRepository.findAllById(dbIds)).thenReturn(List.of(comment1, comment2));
            when(commentMapper.toResponse(comment1)).thenReturn(response1);
            when(commentMapper.toResponse(comment2)).thenReturn(response2);

            List<CommentResponse> result = commentService.getCommentsByUser(userId);

            assertThat(result).containsExactly(response1, response2);

            verify(commentRepository).findIdsByUserId(userId);
            verify(commentIdRedisRepository).saveAllByUserId(userId, dbIds);
            verify(commentRepository).findAllById(dbIds);
            verify(commentCacheRepository).put(response1);
            verify(commentCacheRepository).put(response2);
        }

        @Test
        @DisplayName("캐시에 없는 comment들만 DB에서 조회해서 cache에 저장 후 결과에 추가한다")
        void loadsOnlyMissingCommentsFromDb() {
            Long userId = 100L;
            List<Long> commentIds = List.of(1L, 2L, 3L);

            CommentResponse cachedResponse = mock(CommentResponse.class);
            CommentResponse dbResponse1 = mock(CommentResponse.class);
            CommentResponse dbResponse2 = mock(CommentResponse.class);

            Comment comment2 = mock(Comment.class);
            Comment comment3 = mock(Comment.class);

            when(commentIdRedisRepository.findAllByUserId(userId)).thenReturn(commentIds);
            when(commentCacheRepository.getAll()).thenReturn(Map.of(
                    1L, cachedResponse
            ));

            when(commentRepository.findAllById(List.of(2L, 3L)))
                    .thenReturn(List.of(comment2, comment3));
            when(commentMapper.toResponse(comment2)).thenReturn(dbResponse1);
            when(commentMapper.toResponse(comment3)).thenReturn(dbResponse2);

            List<CommentResponse> result = commentService.getCommentsByUser(userId);

            assertThat(result).containsExactly(cachedResponse, dbResponse1, dbResponse2);

            verify(commentRepository).findAllById(List.of(2L, 3L));
            verify(commentCacheRepository).put(dbResponse1);
            verify(commentCacheRepository).put(dbResponse2);
        }
    }

    @Nested
    @DisplayName("createComment()")
    class CreateCommentTest {

        @Test
        @DisplayName("성공: 댓글 생성, 로그/캐시/redis 반영")
        void createComment_success() {
            Long userId = 1L;
            Long diaryId = 10L;
            Long commentId = 100L;

            CommentRequest request = mock(CommentRequest.class);
            User user = mock(User.class);
            Diary diary = mock(Diary.class);
            CommentResponse commentResponse = mock(CommentResponse.class);
            DiaryResponse diaryResponse = mock(DiaryResponse.class);

            ReflectionTestUtils.setField(diary, "id", diaryId);
            ReflectionTestUtils.setField(diary, "comments", new ArrayList<Comment>());

            given(request.getDiaryId()).willReturn(diaryId);
            given(request.getContent()).willReturn("댓글 내용");

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(diaryRepository.findById(diaryId)).willReturn(Optional.of(diary));

            given(diary.getId()).willReturn(diaryId);
            given(diary.getComments()).willReturn(new ArrayList<>());

            given(commentRepository.save(any(Comment.class))).willAnswer(invocation -> {
                Comment comment = invocation.getArgument(0);
                ReflectionTestUtils.setField(comment, "id", commentId);
                return comment;
            });

            given(snapshotFunc.snapshot(any(Comment.class))).willReturn(Map.of("id", commentId));
            given(diaryMapper.toResponse(diary)).willReturn(diaryResponse);
            given(commentMapper.toResponse(any(Comment.class))).willReturn(commentResponse);

            CommentResponse result = commentService.createComment(userId, request);

            assertEquals(commentResponse, result);

            verify(commentRepository).save(any(Comment.class));
            verify(activityLogService).log(
                    eq(ActivityEntityType.COMMENT),
                    eq(ActivityAction.CREATE),
                    eq(commentId),
                    anyString(),
                    eq(user),
                    isNull(),
                    any()
            );

            verify(diaryCacheRepository).put(diaryResponse);
            verify(commentCacheRepository).put(commentResponse);
            verify(commentIdRedisRepository).addByDiaryId(diaryId, commentId);
            verify(commentIdRedisRepository).addByUserId(userId, commentId);

            assertEquals(1, diary.getComments().size());
        }

        @Test
        @DisplayName("실패: 사용자가 없으면 NotFoundException")
        void createComment_fail_userNotFound() {
            Long userId = 1L;
            CommentRequest request = mock(CommentRequest.class);

            given(userRepository.findById(userId)).willReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> commentService.createComment(userId, request));

            verify(diaryRepository, never()).findById(anyLong());
            verify(commentRepository, never()).save(any(Comment.class));
            verify(activityLogService, never()).log(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("실패: 게시글이 없으면 NotFoundException")
        void createComment_fail_diaryNotFound() {
            Long userId = 1L;
            Long diaryId = 10L;

            CommentRequest request = mock(CommentRequest.class);
            User user = mock(User.class);

            given(request.getDiaryId()).willReturn(diaryId);
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(diaryRepository.findById(diaryId)).willReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> commentService.createComment(userId, request));

            verify(commentRepository, never()).save(any(Comment.class));
            verify(activityLogService, never()).log(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("실패: 댓글 내용이 공란이면 BadRequestException")
        void createComment_fail_blankContent() {
            Long userId = 1L;
            Long diaryId = 10L;

            CommentRequest request = mock(CommentRequest.class);
            User user = mock(User.class);
            Diary diary = mock(Diary.class);

            given(request.getDiaryId()).willReturn(diaryId);
            given(request.getContent()).willReturn("   ");

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(diaryRepository.findById(diaryId)).willReturn(Optional.of(diary));

            assertThrows(BadRequestException.class,
                    () -> commentService.createComment(userId, request));

            verify(commentRepository, never()).save(any(Comment.class));
            verify(activityLogService, never()).log(any(), any(), any(), any(), any(), any(), any());
            verify(diaryCacheRepository, never()).put(any());
            verify(commentCacheRepository, never()).put(any());
        }
    }

    @Nested
    @DisplayName("editComment()")
    class EditCommentTest {

        @Test
        @DisplayName("성공: 댓글 수정, 로그/캐시 반영")
        void editComment_success() {
            Long userId = 1L;
            Long commentId = 20L;

            CommentRequest request = mock(CommentRequest.class);
            User user = mock(User.class);
            Comment comment = mock(Comment.class);
            Diary diary = mock(Diary.class);
            CommentResponse commentResponse = mock(CommentResponse.class);
            DiaryResponse diaryResponse = mock(DiaryResponse.class);

            given(request.getContent()).willReturn("수정된 댓글");
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
            given(commentRepository.findByUserIdAndId(userId, commentId)).willReturn(Optional.of(comment));

            given(comment.getDiary()).willReturn(diary);
            given(comment.getId()).willReturn(commentId);
            given(comment.logMessage()).willReturn("댓글 수정");

            given(snapshotFunc.snapshot(comment)).willReturn(new HashMap<>()).willReturn(new HashMap<>());

            given(diaryMapper.toResponse(diary)).willReturn(diaryResponse);
            given(commentMapper.toResponse(comment)).willReturn(commentResponse);

            CommentResponse result = commentService.editComment(userId, commentId, request);

            assertEquals(commentResponse, result);

            verify(comment).setContent("수정된 댓글");

            verify(activityLogService).log(
                    eq(ActivityEntityType.COMMENT),
                    eq(ActivityAction.UPDATE),
                    eq(commentId),
                    eq("댓글 수정"),
                    eq(user),
                    eq(new HashMap<>()),
                    eq(new HashMap<>())
            );

            verify(diary).addComment(comment);
            verify(diaryCacheRepository).put(diaryResponse);
            verify(commentCacheRepository).put(commentResponse);
        }

        @Test
        @DisplayName("실패: 사용자가 없으면 NotFoundException")
        void editComment_fail_userNotFound() {
            Long userId = 1L;
            Long commentId = 20L;

            CommentRequest request = mock(CommentRequest.class);
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> commentService.editComment(userId, commentId, request));

            verify(commentRepository, never()).findByUserIdAndId(anyLong(), anyLong());
            verify(activityLogService, never()).log(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("실패: 해당 유저 댓글이 아니면 NotFoundException")
        void editComment_fail_commentNotFound() {
            Long userId = 1L;
            Long commentId = 20L;

            CommentRequest request = mock(CommentRequest.class);
            User user = mock(User.class);
            Comment comment = mock(Comment.class);

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
            given(commentRepository.findByUserIdAndId(userId, commentId)).willReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> commentService.editComment(userId, commentId, request));

            verify(activityLogService, never()).log(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("실패: 수정 내용이 공란이면 ConflictException")
        void editComment_fail_blankContent() {
            Long userId = 1L;
            Long commentId = 20L;

            CommentRequest request = mock(CommentRequest.class);
            User user = mock(User.class);
            Comment comment = mock(Comment.class);

            given(request.getContent()).willReturn("   ");
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
            given(commentRepository.findByUserIdAndId(userId, commentId)).willReturn(Optional.of(comment));

            given(snapshotFunc.snapshot(comment)).willReturn(new HashMap<>());

            assertThrows(ConflictException.class,
                    () -> commentService.editComment(userId, commentId, request));

            verify(comment, never()).setContent(any());
            verify(activityLogService, never()).log(any(), any(), any(), any(), any(), any(), any());
            verify(commentCacheRepository, never()).put(any());
            verify(diaryCacheRepository, never()).put(any());
        }
    }

    @Nested
    @DisplayName("deleteComment()")
    class DeleteCommentTest {

        @Test
        @DisplayName("성공: 댓글 삭제, 로그/캐시/redis 반영")
        void deleteComment_success() {
            Long userId = 1L;
            Long commentId = 20L;
            Long diaryId = 10L;

            User user = mock(User.class);
            Comment comment = mock(Comment.class);
            Diary diary = mock(Diary.class);
            DiaryResponse diaryResponse = mock(DiaryResponse.class);

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
            given(commentRepository.findByUserIdAndId(userId, commentId)).willReturn(Optional.of(comment));

            given(comment.getId()).willReturn(commentId);
            given(comment.logMessage()).willReturn("댓글 삭제");
            given(comment.getDiary()).willReturn(diary);
            given(diary.getId()).willReturn(diaryId);

            given(snapshotFunc.snapshot(comment)).willReturn(new HashMap<>());

            given(diaryMapper.toResponse(diary)).willReturn(diaryResponse);

            commentService.deleteComment(userId, commentId);

            verify(activityLogService).log(
                    eq(ActivityEntityType.COMMENT),
                    eq(ActivityAction.DELETE),
                    eq(commentId),
                    eq("댓글 삭제"),
                    eq(user),
                    eq(new HashMap<>()),
                    isNull()
            );

            verify(diary).removeComment(comment);
            verify(diaryCacheRepository).put(diaryResponse);

            verify(commentCacheRepository).evict(commentId);
            verify(commentIdRedisRepository).removeByDiaryId(diaryId, commentId);
            verify(commentIdRedisRepository).removeByUserId(userId, commentId);

            verify(commentRepository).delete(comment);
        }

        @Test
        @DisplayName("실패: 사용자 없음 -> NotFoundException 으로 래핑")
        void deleteComment_fail_userNotFound_wrappedConflict() {
            Long userId = 1L;
            Long commentId = 20L;

            given(userRepository.findById(userId)).willReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> commentService.deleteComment(userId, commentId));

            verify(commentRepository, never()).findByUserIdAndId(anyLong(), anyLong());
            verify(commentRepository, never()).delete(any());
        }

        @Test
        @DisplayName("실패: 댓글 없음 -> NotFoundException 으로 래핑")
        void deleteComment_fail_commentNotFound_wrappedConflict() {
            Long userId = 1L;
            Long commentId = 20L;

            User user = mock(User.class);
            Comment comment = mock(Comment.class);

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
            given(commentRepository.findByUserIdAndId(userId, commentId)).willReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> commentService.deleteComment(userId, commentId));

            verify(commentRepository, never()).delete(any());
            verify(activityLogService, never()).log(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("실패: delete 중 예외 발생 -> ConflictException")
        void deleteComment_fail_deleteException() {
            Long userId = 1L;
            Long commentId = 20L;
            Long diaryId = 10L;

            User user = mock(User.class);
            Comment comment = mock(Comment.class);
            Diary diary = mock(Diary.class);
            DiaryResponse diaryResponse = mock(DiaryResponse.class);

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
            given(commentRepository.findByUserIdAndId(userId, commentId)).willReturn(Optional.of(comment));

            given(comment.getId()).willReturn(commentId);
            given(comment.logMessage()).willReturn("댓글 삭제");
            given(comment.getDiary()).willReturn(diary);
            given(diary.getId()).willReturn(diaryId);
            given(snapshotFunc.snapshot(comment)).willReturn(new HashMap<>());
            given(diaryMapper.toResponse(diary)).willReturn(diaryResponse);

            doThrow(new RuntimeException("db error")).when(commentRepository).delete(comment);

            assertThrows(ConflictException.class,
                    () -> commentService.deleteComment(userId, commentId));
        }
    }
}