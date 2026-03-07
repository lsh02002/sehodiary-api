package com.shop.sehodiary_api.web.mapper.diaryemotion;

import com.shop.sehodiary_api.repository.diaryEmotion.DiaryEmotion;
import com.shop.sehodiary_api.web.dto.diaryemotion.DiaryEmotionResponse;
import org.springframework.stereotype.Component;

@Component
public class DiaryEmotionMapper {
    public DiaryEmotionResponse toResponse(DiaryEmotion diaryEmotion) {
        return DiaryEmotionResponse.builder()
                .id(diaryEmotion.getId())
                .diaryId(diaryEmotion.getDiary() != null ? diaryEmotion.getDiary().getId() : null)
                .nickname(
                        diaryEmotion.getDiary() != null && diaryEmotion.getDiary().getUser() != null
                                ? diaryEmotion.getDiary().getUser().getNickname()
                                : null
                )
                .name(diaryEmotion.getEmotion() != null ? diaryEmotion.getEmotion().getName() : null)
                .emoji(diaryEmotion.getEmotion() != null ? diaryEmotion.getEmotion().getEmoji() : null)
                .createdAt(diaryEmotion.getCreatedAt().toString())
                .updatedAt(diaryEmotion.getUpdatedAt().toString())
                .build();
    }
}
