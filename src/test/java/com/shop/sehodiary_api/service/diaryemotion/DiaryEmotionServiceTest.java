package com.shop.sehodiary_api.service.diaryemotion;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.util.*;

import com.shop.sehodiary_api.repository.activity.ActivityAction;
import com.shop.sehodiary_api.repository.activity.ActivityEntityType;
import com.shop.sehodiary_api.repository.activity.function.SnapshotFunc;
import com.shop.sehodiary_api.repository.diary.Diary;
import com.shop.sehodiary_api.repository.diary.DiaryRepository;
import com.shop.sehodiary_api.repository.diaryEmotion.DiaryEmotion;
import com.shop.sehodiary_api.repository.diaryEmotion.DiaryEmotionRepository;
import com.shop.sehodiary_api.repository.emotion.Emotion;
import com.shop.sehodiary_api.repository.emotion.EmotionRepository;
import com.shop.sehodiary_api.repository.user.User;
import com.shop.sehodiary_api.repository.user.UserRepository;
import com.shop.sehodiary_api.service.activelog.ActivityLogService;
import com.shop.sehodiary_api.service.exceptions.ConflictException;
import com.shop.sehodiary_api.service.exceptions.NotAcceptableException;
import com.shop.sehodiary_api.service.exceptions.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DiaryEmotionServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DiaryRepository diaryRepository;

    @Mock
    private EmotionRepository emotionRepository;

    @Mock
    private DiaryEmotionRepository diaryemotionRepository;

    @Mock
    private SnapshotFunc snapshotFunc;

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private DiaryEmotionService diaryEmotionService;

    @Nested
    @DisplayName("createDiaryEmotion()")
    class CreateDiaryEmotionTest {

        @Test
        @DisplayName("성공: 일기 이모션 생성 및 로그 저장")
        void createDiaryEmotion_success() {
            Long userId = 1L;
            Long diaryId = 10L;
            String emoji = "😊";

            User user = mock(User.class);
            Diary diary = mock(Diary.class);
            Emotion emotion = mock(Emotion.class);
            DiaryEmotion savedDiaryEmotion = mock(DiaryEmotion.class);

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(diaryRepository.findById(diaryId)).willReturn(Optional.of(diary));
            given(diaryRepository.findByUserIdAndId(userId, diaryId)).willReturn(Optional.of(diary));
            given(emotionRepository.findByEmoji(emoji)).willReturn(Optional.of(emotion));

            given(diaryemotionRepository.save(any(DiaryEmotion.class))).willReturn(savedDiaryEmotion);
            given(savedDiaryEmotion.getId()).willReturn(100L);
            given(savedDiaryEmotion.logMessage()).willReturn("다이어리 이모션 생성");

            given(snapshotFunc.snapshot(savedDiaryEmotion)).willReturn(new HashMap<>());

            diaryEmotionService.createDiaryEmotion(userId, diaryId, emoji);

            verify(userRepository).findById(userId);
            verify(diaryRepository).findByUserIdAndId(userId, diaryId);
            verify(emotionRepository).findByEmoji(emoji);
            verify(diaryemotionRepository).save(any(DiaryEmotion.class));
            verify(diary).addDiaryEmotion(savedDiaryEmotion);

            verify(activityLogService).log(
                    eq(ActivityEntityType.DIARY_EMOTION),
                    eq(ActivityAction.CREATE),
                    eq(100L),
                    eq("다이어리 이모션 생성"),
                    eq(user),
                    isNull(),
                    eq(new HashMap<>())
            );
        }

        @Test
        @DisplayName("실패: 사용자가 없으면 NotFoundException")
        void createDiaryEmotion_fail_userNotFound() {
            Long userId = 1L;
            Long diaryId = 10L;
            String emoji = "😊";

            given(userRepository.findById(userId)).willReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> diaryEmotionService.createDiaryEmotion(userId, diaryId, emoji));

            verify(diaryRepository, never()).findByUserIdAndId(anyLong(), anyLong());
            verify(emotionRepository, never()).findByEmoji(anyString());
            verify(diaryemotionRepository, never()).save(any());
            verify(activityLogService, never()).log(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("실패: 해당 유저의 일기가 아니면 NotFoundException")
        void createDiaryEmotion_fail_diaryNotFound() {
            Long userId = 1L;
            Long diaryId = 10L;
            String emoji = "😊";

            User user = mock(User.class);
            Diary diary = mock(Diary.class);

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(diaryRepository.findById(diaryId)).willReturn(Optional.of(diary));
            given(diaryRepository.findByUserIdAndId(userId, diaryId)).willReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> diaryEmotionService.createDiaryEmotion(userId, diaryId, emoji));

            verify(emotionRepository, never()).findByEmoji(anyString());
            verify(diaryemotionRepository, never()).save(any());
            verify(activityLogService, never()).log(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("실패: 이모지가 없으면 NotFoundException")
        void createDiaryEmotion_fail_emotionNotFound() {
            Long userId = 1L;
            Long diaryId = 10L;
            String emoji = "😊";

            User user = mock(User.class);
            Diary diary = mock(Diary.class);

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(diaryRepository.findById(diaryId)).willReturn(Optional.of(diary));
            given(diaryRepository.findByUserIdAndId(userId, diaryId)).willReturn(Optional.of(diary));
            given(emotionRepository.findByEmoji(emoji)).willReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> diaryEmotionService.createDiaryEmotion(userId, diaryId, emoji));

            verify(diaryemotionRepository, never()).save(any());
            verify(activityLogService, never()).log(any(), any(), any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("editDiaryEmotion()")
    class EditDiaryEmotionTest {

        @Test
        @DisplayName("성공: 기존 이모션이 없으면 새로 생성")
        void editDiaryEmotion_createWhenEmpty() {
            Long userId = 1L;
            Long diaryId = 10L;
            String emoji = "😊";

            User user = mock(User.class);
            Emotion emotion = mock(Emotion.class);
            Diary diary = mock(Diary.class);
            DiaryEmotion savedDiaryEmotion = mock(DiaryEmotion.class);

            List<DiaryEmotion> diaryEmotions = new ArrayList<>();

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(diaryRepository.findById(diaryId)).willReturn(Optional.of(diary));
            given(diaryRepository.findByUserIdAndId(userId, diaryId)).willReturn(Optional.of(diary));
            given(diary.getDiaryEmotions()).willReturn(diaryEmotions);
            given(emotionRepository.findByEmoji(emoji)).willReturn(Optional.of(emotion));
            given(diaryemotionRepository.save(any(DiaryEmotion.class))).willReturn(savedDiaryEmotion);

            given(savedDiaryEmotion.getId()).willReturn(200L);
            given(savedDiaryEmotion.logMessage()).willReturn("다이어리 이모션 수정");

            given(snapshotFunc.snapshot(null)).willReturn(null);
            given(snapshotFunc.snapshot(savedDiaryEmotion)).willReturn(new HashMap<>());

            diaryEmotionService.editDiaryEmotion(userId, diaryId, emoji);

            verify(diaryemotionRepository).save(any(DiaryEmotion.class));
            verify(diary).addDiaryEmotion(savedDiaryEmotion);

            verify(activityLogService).log(
                    eq(ActivityEntityType.DIARY_EMOTION),
                    eq(ActivityAction.UPDATE),
                    eq(200L),
                    eq("다이어리 이모션 수정"),
                    eq(user),
                    isNull(),
                    eq(new HashMap<>())
            );
        }

        @Test
        @DisplayName("성공: 기존 이모션과 다른 이모션이면 수정")
        void editDiaryEmotion_updateWhenDifferentEmotion() {
            Long userId = 1L;
            Long diaryId = 10L;
            String emoji = "😊";

            User user = mock(User.class);
            Diary diary = mock(Diary.class);
            DiaryEmotion diaryEmotion = mock(DiaryEmotion.class);
            Emotion oldEmotion = mock(Emotion.class);
            Emotion newEmotion = mock(Emotion.class);

            Map<String, Object> beforeobject = new HashMap<>();
            beforeobject.put("emotion", "sad");

            Map<String, Object> afterobject = new HashMap<>();
            afterobject.put("emotion", "happy");

            List<DiaryEmotion> diaryEmotions = List.of(diaryEmotion);

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(diaryRepository.findById(diaryId)).willReturn(Optional.of(diary));
            given(diaryRepository.findByUserIdAndId(userId, diaryId)).willReturn(Optional.of(diary));
            given(diary.getDiaryEmotions()).willReturn(diaryEmotions);

            given(diaryEmotion.getEmotion()).willReturn(oldEmotion);
            given(oldEmotion.getId()).willReturn(1L);

            given(emotionRepository.findByEmoji(emoji)).willReturn(Optional.of(newEmotion));
            given(newEmotion.getId()).willReturn(2L);

            given(diaryEmotion.getId()).willReturn(300L);
            given(diaryEmotion.logMessage()).willReturn("다이어리 이모션 수정");

            given(snapshotFunc.snapshot(diaryEmotion))
                    .willReturn(beforeobject)
                    .willReturn(afterobject);

            diaryEmotionService.editDiaryEmotion(userId, diaryId, emoji);

            verify(diaryEmotion).setEmotion(newEmotion);
            verify(activityLogService).log(
                    eq(ActivityEntityType.DIARY_EMOTION),
                    eq(ActivityAction.UPDATE),
                    eq(300L),
                    eq("다이어리 이모션 수정"),
                    eq(user),
                    eq(beforeobject),
                    eq(afterobject)
            );
        }

        @Test
        @DisplayName("성공: 기존 이모션과 같은 이모션이면 로그 저장 안함")
        void editDiaryEmotion_sameEmotion_noLog() {
            Long userId = 1L;
            Long diaryId = 10L;
            String emoji = "😊";

            User user = mock(User.class);
            Diary diary = mock(Diary.class);
            DiaryEmotion diaryEmotion = mock(DiaryEmotion.class);
            Emotion currentEmotion = mock(Emotion.class);
            Emotion sameEmotion = mock(Emotion.class);

            List<DiaryEmotion> diaryEmotions = List.of(diaryEmotion);

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(diaryRepository.findById(diaryId)).willReturn(Optional.of(diary));
            given(diaryRepository.findByUserIdAndId(userId, diaryId)).willReturn(Optional.of(diary));
            given(diary.getDiaryEmotions()).willReturn(diaryEmotions);

            given(diaryEmotion.getEmotion()).willReturn(currentEmotion);
            given(currentEmotion.getId()).willReturn(1L);

            given(emotionRepository.findByEmoji(emoji)).willReturn(Optional.of(sameEmotion));
            given(sameEmotion.getId()).willReturn(1L);

            given(snapshotFunc.snapshot(diaryEmotion))
                    .willReturn(new HashMap<>())
                    .willReturn(new HashMap<>());

            diaryEmotionService.editDiaryEmotion(userId, diaryId, emoji);

            verify(diaryEmotion, never()).setEmotion(any());
            verify(activityLogService, never()).log(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("실패: 이모션이 2개 이상이면 NotAcceptableException")
        void editDiaryEmotion_fail_whenMoreThanOneEmotion() {
            Long userId = 1L;
            Long diaryId = 10L;
            String emoji = "😊";

            User user = mock(User.class);
            Diary diary = mock(Diary.class);

            List<DiaryEmotion> diaryEmotions = List.of(mock(DiaryEmotion.class), mock(DiaryEmotion.class));

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(diaryRepository.findById(diaryId)).willReturn(Optional.of(diary));
            given(diaryRepository.findByUserIdAndId(userId, diaryId)).willReturn(Optional.of(diary));
            given(diary.getDiaryEmotions()).willReturn(diaryEmotions);

            assertThrows(NotAcceptableException.class,
                    () -> diaryEmotionService.editDiaryEmotion(userId, diaryId, emoji));

            verify(emotionRepository, never()).findByEmoji(anyString());
            verify(activityLogService, never()).log(any(), any(), any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("deleteDiaryEmotion()")
    class DeleteDiaryEmotionTest {

        @Test
        @DisplayName("성공: 이모션 삭제 및 로그 저장")
        void deleteDiaryEmotion_success() {
            Long userId = 1L;
            Long diaryId = 10L;
            String emoji = "😊";

            User user = mock(User.class);
            Diary diary = mock(Diary.class);
            Emotion emotion = mock(Emotion.class);
            DiaryEmotion diaryEmotion = mock(DiaryEmotion.class);

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(diaryRepository.findById(diaryId)).willReturn(Optional.of(diary));
            given(emotionRepository.findByEmoji(emoji)).willReturn(Optional.of(emotion));
            given(diaryemotionRepository.findByDiaryIdAndEmotionName(diaryId, emoji)).willReturn(Optional.of(diaryEmotion));

            given(diaryEmotion.getId()).willReturn(400L);
            given(diaryEmotion.logMessage()).willReturn("다이어리 이모션 삭제");

            given(snapshotFunc.snapshot(diaryEmotion)).willReturn(new HashMap<>());

            diaryEmotionService.deleteDiaryEmotion(userId, diaryId, emoji);

            verify(activityLogService).log(
                    eq(ActivityEntityType.DIARY_EMOTION),
                    eq(ActivityAction.DELETE),
                    eq(400L),
                    eq("다이어리 이모션 삭제"),
                    eq(user),
                    eq(new HashMap<>()),
                    isNull()
            );

            verify(emotionRepository).delete(emotion);
        }

        @Test
        @DisplayName("실패: 내부 RuntimeException 발생 시 ConflictException")
        void deleteDiaryEmotion_fail_conflictException() {
            Long userId = 1L;
            Long diaryId = 10L;
            String emoji = "😊";

            given(userRepository.findById(userId)).willThrow(new RuntimeException("db error"));

            assertThrows(ConflictException.class,
                    () -> diaryEmotionService.deleteDiaryEmotion(userId, diaryId, emoji));

            verify(activityLogService, never()).log(any(), any(), any(), any(), any(), any(), any());
            verify(emotionRepository, never()).delete(any());
        }

        @Test
        @DisplayName("실패: 삭제 대상 이모션이 없으면 최종적으로 ConflictException")
        void deleteDiaryEmotion_fail_notFoundWrappedToConflict() {
            Long userId = 1L;
            Long diaryId = 10L;
            String emoji = "😊";

            User user = mock(User.class);
            Diary diary = mock(Diary.class);
            Emotion emotion = mock(Emotion.class);

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(diaryRepository.findById(diaryId)).willReturn(Optional.of(diary));
            given(emotionRepository.findByEmoji(emoji)).willReturn(Optional.of(emotion));
            given(diaryemotionRepository.findByDiaryIdAndEmotionName(diaryId, emoji)).willReturn(Optional.empty());

            assertThrows(ConflictException.class,
                    () -> diaryEmotionService.deleteDiaryEmotion(userId, diaryId, emoji));

            verify(emotionRepository, never()).delete(any());
            verify(activityLogService, never()).log(any(), any(), any(), any(), any(), any(), any());
        }
    }
}