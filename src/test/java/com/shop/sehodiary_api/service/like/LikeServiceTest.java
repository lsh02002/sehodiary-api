package com.shop.sehodiary_api.service.like;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import com.shop.sehodiary_api.repository.activity.ActivityAction;
import com.shop.sehodiary_api.repository.activity.ActivityEntityType;
import com.shop.sehodiary_api.repository.activity.function.SnapshotFunc;
import com.shop.sehodiary_api.repository.diary.Diary;
import com.shop.sehodiary_api.repository.diary.DiaryCacheRepository;
import com.shop.sehodiary_api.repository.diary.DiaryRepository;
import com.shop.sehodiary_api.repository.like.Like;
import com.shop.sehodiary_api.repository.like.LikeRepository;
import com.shop.sehodiary_api.repository.user.User;
import com.shop.sehodiary_api.repository.user.UserRepository;
import com.shop.sehodiary_api.service.activelog.ActivityLogService;
import com.shop.sehodiary_api.service.exceptions.ConflictException;
import com.shop.sehodiary_api.service.exceptions.NotFoundException;
import com.shop.sehodiary_api.web.dto.diary.DiaryResponse;
import com.shop.sehodiary_api.web.mapper.diary.DiaryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private DiaryRepository diaryRepository;
    @Mock
    private LikeRepository likeRepository;
    @Mock
    private DiaryMapper diaryMapper;
    @Mock
    private ActivityLogService activityLogService;
    @Mock
    private SnapshotFunc snapshotFunc;
    @Mock
    private DiaryCacheRepository diaryCacheRepository;

    @InjectMocks
    private LikeService likeService;

    @Nested
    @DisplayName("getLikingNicknamesByDiary()")
    class GetLikingNicknamesByDiaryTest {

        @Test
        @DisplayName("해당 diary에 좋아요를 누른 유저들의 닉네임 목록을 반환한다")
        void returnsNicknames() {
            Long diaryId = 1L;

            User user1 = mock(User.class);
            User user2 = mock(User.class);
            Like like1 = mock(Like.class);
            Like like2 = mock(Like.class);

            when(user1.getNickname()).thenReturn("철수");
            when(user2.getNickname()).thenReturn("영희");
            when(like1.getUser()).thenReturn(user1);
            when(like2.getUser()).thenReturn(user2);
            when(likeRepository.findByDiaryId(diaryId)).thenReturn(List.of(like1, like2));

            List<String> result = likeService.getLikingNicknamesByDiary(diaryId);

            assertThat(result).containsExactly("철수", "영희");
            verify(likeRepository).findByDiaryId(diaryId);
        }

        @Test
        @DisplayName("좋아요가 없으면 빈 리스트를 반환한다")
        void returnsEmptyListWhenNoLikes() {
            Long diaryId = 1L;

            when(likeRepository.findByDiaryId(diaryId)).thenReturn(List.of());

            List<String> result = likeService.getLikingNicknamesByDiary(diaryId);

            assertThat(result).isEmpty();
            verify(likeRepository).findByDiaryId(diaryId);
        }
    }

    @Nested
    @DisplayName("isLiked()")
    class IsLikedTest {

        @Test
        @DisplayName("좋아요가 존재하면 true를 반환한다")
        void returnsTrueWhenLiked() {
            Long userId = 1L;
            Long diaryId = 2L;

            when(likeRepository.existsByUserIdAndDiaryId(userId, diaryId)).thenReturn(true);

            Boolean result = likeService.isLiked(userId, diaryId);

            assertThat(result).isTrue();
            verify(likeRepository).existsByUserIdAndDiaryId(userId, diaryId);
        }

        @Test
        @DisplayName("좋아요가 존재하지 않으면 false를 반환한다")
        void returnsFalseWhenNotLiked() {
            Long userId = 1L;
            Long diaryId = 2L;

            when(likeRepository.existsByUserIdAndDiaryId(userId, diaryId)).thenReturn(false);

            Boolean result = likeService.isLiked(userId, diaryId);

            assertThat(result).isFalse();
            verify(likeRepository).existsByUserIdAndDiaryId(userId, diaryId);
        }
    }

    @Nested
    @DisplayName("insert()")
    class InsertTest {

        @Test
        @DisplayName("정상적으로 좋아요를 저장하고 로그 및 diary cache를 갱신한다")
        void insertSuccess() {
            Long userId = 1L;
            Long diaryId = 2L;

            User user = mock(User.class);
            Diary diary = mock(Diary.class);
            Like savedLike = mock(Like.class);
            DiaryResponse response = mock(DiaryResponse.class);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(diaryRepository.findById(diaryId)).thenReturn(Optional.of(diary));
            when(likeRepository.findByUserIdAndDiaryId(userId, diaryId)).thenReturn(Optional.empty());
            when(likeRepository.save(any(Like.class))).thenReturn(savedLike);

            when(snapshotFunc.snapshot(any(Like.class))).thenReturn(new HashMap<>());
            when(diaryMapper.toResponse(diary)).thenReturn(response);

            Boolean result = likeService.insert(userId, diaryId);

            assertThat(result).isTrue();

            verify(userRepository).findById(userId);
            verify(diaryRepository).findById(diaryId);
            verify(likeRepository).findByUserIdAndDiaryId(userId, diaryId);
            verify(likeRepository).save(any(Like.class));
            verify(activityLogService).log(
                    eq(ActivityEntityType.LIKE),
                    eq(ActivityAction.CREATE),
                    isNull(),
                    anyString(),
                    eq(user),
                    isNull(),
                    eq(new HashMap<>())
            );
            verify(diaryMapper).toResponse(diary);
            verify(diaryCacheRepository).put(response);
        }

        @Test
        @DisplayName("유저가 없으면 NotFoundException을 던진다")
        void throwsWhenUserNotFound() {
            Long userId = 1L;
            Long diaryId = 2L;

            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> likeService.insert(userId, diaryId))
                    .isInstanceOf(NotFoundException.class)
                    .extracting("detailMessage")
                    .isEqualTo("해당 사용자를 찾을 수 없습니다.");

            verify(diaryRepository, never()).findById(anyLong());
            verify(likeRepository, never()).save(any());
        }

        @Test
        @DisplayName("글이 없으면 NotFoundException을 던진다")
        void throwsWhenDiaryNotFound() {
            Long userId = 1L;
            Long diaryId = 2L;

            User user = mock(User.class);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(diaryRepository.findById(diaryId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> likeService.insert(userId, diaryId))
                    .isInstanceOf(NotFoundException.class)
                    .extracting("detailMessage")
                    .isEqualTo("해당 글을 찾을 수 없습니다.");

            verify(likeRepository, never()).findByUserIdAndDiaryId(anyLong(), anyLong());
            verify(likeRepository, never()).save(any());
        }

        @Test
        @DisplayName("이미 좋아요가 되어 있으면 ConflictException을 던진다")
        void throwsWhenAlreadyLiked() {
            Long userId = 1L;
            Long diaryId = 2L;

            User user = mock(User.class);
            Diary diary = mock(Diary.class);
            Like existingLike = mock(Like.class);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(diaryRepository.findById(diaryId)).thenReturn(Optional.of(diary));
            when(likeRepository.findByUserIdAndDiaryId(userId, diaryId)).thenReturn(Optional.of(existingLike));

            assertThatThrownBy(() -> likeService.insert(userId, diaryId))
                    .isInstanceOf(ConflictException.class)
                    .extracting("detailMessage")
                    .isEqualTo("이미 좋아요가 되어있습니다.");

            verify(likeRepository, never()).save(any());
            verify(activityLogService, never()).log(any(), any(), anyLong(), any(), any(), any(), any());
            verify(diaryCacheRepository, never()).put(any());
        }
    }

    @Nested
    @DisplayName("delete()")
    class DeleteTest {

        @Test
        @DisplayName("정상적으로 좋아요를 삭제하고 로그 및 diary cache를 갱신한다")
        void deleteSuccess() {
            Long userId = 1L;
            Long diaryId = 2L;

            User user = mock(User.class);
            Diary diary = mock(Diary.class);
            Like like = mock(Like.class);
            DiaryResponse response = mock(DiaryResponse.class);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(diaryRepository.findById(diaryId)).thenReturn(Optional.of(diary));
            when(likeRepository.findByUserIdAndDiaryId(userId, diaryId)).thenReturn(Optional.of(like));

            when(like.getId()).thenReturn(101L);
            when(like.logMessage()).thenReturn("like-delete");
            when(snapshotFunc.snapshot(like)).thenReturn(new HashMap<>());
            when(diaryMapper.toResponse(diary)).thenReturn(response);

            Boolean result = likeService.delete(userId, diaryId);

            assertThat(result).isFalse();

            verify(userRepository).findById(userId);
            verify(diaryRepository).findById(diaryId);
            verify(likeRepository).findByUserIdAndDiaryId(userId, diaryId);
            verify(activityLogService).log(
                    eq(ActivityEntityType.LIKE),
                    eq(ActivityAction.DELETE),
                    eq(101L),
                    eq("like-delete"),
                    eq(user),
                    eq(new HashMap<>()),
                    isNull()
            );
            verify(diary).removeLike(like);
            verify(likeRepository).delete(like);
            verify(diaryMapper).toResponse(diary);
            verify(diaryCacheRepository).put(response);
        }

        @Test
        @DisplayName("유저가 없으면 NotFoundException을 던진다")
        void throwsWhenUserNotFound() {
            Long userId = 1L;
            Long diaryId = 2L;

            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> likeService.delete(userId, diaryId))
                    .isInstanceOf(NotFoundException.class)
                    .extracting("detailMessage")
                    .isEqualTo("해당 사용자를 찾을 수 없습니다.");

            verify(diaryRepository, never()).findById(anyLong());
            verify(likeRepository, never()).findByUserIdAndDiaryId(anyLong(), anyLong());
        }

        @Test
        @DisplayName("글이 없으면 NotFoundException을 던진다")
        void throwsWhenDiaryNotFound() {
            Long userId = 1L;
            Long diaryId = 2L;
            User user = mock(User.class);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(diaryRepository.findById(diaryId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> likeService.delete(userId, diaryId))
                    .isInstanceOf(NotFoundException.class)
                    .extracting("detailMessage")
                    .isEqualTo("해당 글을 찾을 수 없습니다.");

            verify(likeRepository, never()).findByUserIdAndDiaryId(anyLong(), anyLong());
            verify(likeRepository, never()).delete(any());
        }

        @Test
        @DisplayName("좋아요가 없으면 NotFoundException을 던진다")
        void throwsWhenLikeNotFound() {
            Long userId = 1L;
            Long diaryId = 2L;

            User user = mock(User.class);
            Diary diary = mock(Diary.class);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(diaryRepository.findById(diaryId)).thenReturn(Optional.of(diary));
            when(likeRepository.findByUserIdAndDiaryId(userId, diaryId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> likeService.delete(userId, diaryId))
                    .isInstanceOf(NotFoundException.class)
                    .extracting("detailMessage")
                    .isEqualTo("좋아요가 되어있지 않았습니다.");

            verify(likeRepository, never()).delete(any());
            verify(activityLogService, never()).log(any(), any(), anyLong(), any(), any(), any(), any());
            verify(diaryCacheRepository, never()).put(any());
        }
    }

    @Nested
    @DisplayName("getMyLikedDiaries()")
    class GetMyLikedDiariesTest {

        @Test
        @DisplayName("내가 좋아요한 diary 목록을 DiaryResponse로 변환해 반환한다")
        void returnsLikedDiaries() {
            Long userId = 1L;

            Diary diary1 = mock(Diary.class);
            Diary diary2 = mock(Diary.class);
            Like like1 = mock(Like.class);
            Like like2 = mock(Like.class);
            DiaryResponse response1 = mock(DiaryResponse.class);
            DiaryResponse response2 = mock(DiaryResponse.class);

            when(like1.getDiary()).thenReturn(diary1);
            when(like2.getDiary()).thenReturn(diary2);
            when(likeRepository.findAllByUserId(userId)).thenReturn(List.of(like1, like2));
            when(diaryMapper.toResponse(diary1)).thenReturn(response1);
            when(diaryMapper.toResponse(diary2)).thenReturn(response2);

            List<DiaryResponse> result = likeService.getMyLikedDiaries(userId);

            assertThat(result).containsExactly(response1, response2);
            verify(likeRepository).findAllByUserId(userId);
            verify(diaryMapper).toResponse(diary1);
            verify(diaryMapper).toResponse(diary2);
        }

        @Test
        @DisplayName("좋아요한 diary가 없으면 빈 리스트를 반환한다")
        void returnsEmptyListWhenNoLikedDiaries() {
            Long userId = 1L;

            when(likeRepository.findAllByUserId(userId)).thenReturn(List.of());

            List<DiaryResponse> result = likeService.getMyLikedDiaries(userId);

            assertThat(result).isEmpty();
            verify(likeRepository).findAllByUserId(userId);
            verify(diaryMapper, never()).toResponse(any());
        }
    }
}