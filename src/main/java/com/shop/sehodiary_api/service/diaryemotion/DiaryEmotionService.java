package com.shop.sehodiary_api.service.diaryemotion;

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
import com.shop.sehodiary_api.web.dto.diaryemotion.DiaryEmotionResponse;
import com.shop.sehodiary_api.web.mapper.diaryemotion.DiaryEmotionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DiaryEmotionService {
    private final UserRepository userRepository;
    private final DiaryRepository diaryRepository;
    private final EmotionRepository emotionRepository;
    private final DiaryEmotionRepository diaryemotionRepository;
    private final ActivityLogService activityLogService;
    private final SnapshotFunc snapshotFunc;
    private final DiaryEmotionMapper diaryEmotionMapper;

    @Transactional
    public List<DiaryEmotionResponse> getEmotionsByDiary(Long diaryId) {
        return diaryemotionRepository.findByDiaryId(diaryId)
                .stream().map(diaryEmotionMapper::toResponse).toList();
    }

    @Transactional
    public void createDiaryEmotion(Long userId, Long diaryId, String emoji) {
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new NotFoundException("해당 사용자를 찾을 수 없습니다.", userId));

        diaryRepository.findById(diaryId)
                .orElseThrow(() -> new NotFoundException("해당 글을 찾을 수 없습니다.", diaryId));

        Diary diary = diaryRepository.findByUserIdAndId(userId, diaryId)
                .orElseThrow(()->new NotFoundException("해당 사용자가 작성한 글이 아닙니다", diaryId));

        Emotion emotion = emotionRepository.findByEmoji(emoji)
                        .orElseThrow(()->new NotFoundException("해당 이모지를 찾을 수 없습니다.", emoji));

        DiaryEmotion diaryEmotion = diaryemotionRepository.save(DiaryEmotion.builder()
                        .diary(diary)
                        .emotion(emotion)
                .build());

        diary.addDiaryEmotion(diaryEmotion);

        Object afterdiaryemotion = snapshotFunc.snapshot(diaryEmotion);

        activityLogService.log(ActivityEntityType.DIARY_EMOTION, ActivityAction.CREATE, diaryEmotion.getId(), diaryEmotion.logMessage(), user, null, afterdiaryemotion);
    }

    @Transactional
    public void editDiaryEmotion(Long userId, Long diaryId, String emoji) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("해당 사용자를 찾을 수 없습니다.", userId));

        diaryRepository.findById(diaryId)
                .orElseThrow(() -> new NotFoundException("해당 글을 찾을 수 없습니다.", diaryId));

        Diary diary = diaryRepository.findByUserIdAndId(userId, diaryId)
                .orElseThrow(() -> new NotFoundException("해당 사용자가 작성한 글이 아닙니다", diaryId));

        List<DiaryEmotion> diaryEmotions = diary.getDiaryEmotions();

        if (diaryEmotions.size() > 1) {
            throw new NotAcceptableException("해당 글의 이모션이 한개가 아닙니다.", null);
        }

        DiaryEmotion diaryEmotion = diaryEmotions.isEmpty() ? null : diaryEmotions.get(0);
        Object beforeDiaryEmotion = snapshotFunc.snapshot(diaryEmotion);

        Emotion emotion = emotionRepository.findByEmoji(emoji)
                .orElseThrow(() -> new NotFoundException("해당 이모지를 찾을 수 없습니다.", emoji));

        if(diaryEmotions.isEmpty()) {
            DiaryEmotion savedDiaryEmotion = diaryemotionRepository.save(DiaryEmotion.builder()
                    .diary(diary)
                    .emotion(emotion)
                    .build());

            diary.addDiaryEmotion(savedDiaryEmotion);
            diaryEmotion = savedDiaryEmotion;

        } else if (!Objects.equals(Objects.requireNonNull(diaryEmotion).getEmotion().getId(), emotion.getId())) {
            diaryEmotion.setEmotion(emotion);
        }

        Object afterDiaryEmotion = snapshotFunc.snapshot(diaryEmotion);

        if(!Objects.equals(beforeDiaryEmotion, afterDiaryEmotion)) {
            activityLogService.log(
                    ActivityEntityType.DIARY_EMOTION,
                    ActivityAction.UPDATE,
                    diaryEmotion.getId(),
                    diaryEmotion.logMessage(),
                    user,
                    beforeDiaryEmotion,
                    afterDiaryEmotion
            );
        }
    }

    @Transactional
    public void deleteDiaryEmotion(Long userId, Long diaryId, String emoji) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(()-> new NotFoundException("해당 사용자를 찾을 수 없습니다.", userId));

            diaryRepository.findById(diaryId)
                    .orElseThrow(() -> new NotFoundException("해당 글을 찾을 수 없습니다.", diaryId));

            Emotion emotion = emotionRepository.findByEmoji(emoji)
                    .orElseThrow(()->new NotFoundException("해당 이모션을 찾을 수 없습니다", emoji));

            DiaryEmotion diaryEmotion = diaryemotionRepository.findByDiaryIdAndEmotionName(diaryId, emoji)
                    .orElseThrow(()->new NotFoundException("해당 글의 이모지를 찾을 수 없습니다", emoji));

            Object beforediaryemotion = snapshotFunc.snapshot(diaryEmotion);

            activityLogService.log(ActivityEntityType.DIARY_EMOTION, ActivityAction.DELETE, diaryEmotion.getId(), diaryEmotion.logMessage(), user, beforediaryemotion, null);

            emotionRepository.delete(emotion);
        } catch (RuntimeException e) {
            throw new ConflictException("해당 글의 이모션을 삭제할 수 없습니다", null);
        }
    }
}
