package com.shop.sehodiary_api.web.mapper.emotion;

import com.shop.sehodiary_api.repository.emotion.Emotion;
import com.shop.sehodiary_api.web.dto.emotion.EmotionResponse;
import org.springframework.stereotype.Component;

@Component
public class EmotionMapper {
    public EmotionResponse toResponse(Emotion emotion) {
        return EmotionResponse.builder()
                .id(emotion.getId())
                .name(emotion.getName())
                .emoji(emotion.getEmoji())
                .createdAt(emotion.getCreatedAt().toString())
                .updatedAt(emotion.getUpdatedAt().toString())
                .build();
    }
}
