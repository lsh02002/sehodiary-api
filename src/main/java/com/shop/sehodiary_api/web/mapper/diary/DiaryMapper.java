package com.shop.sehodiary_api.web.mapper.diary;

import com.shop.sehodiary_api.repository.diary.Diary;
import com.shop.sehodiary_api.web.dto.diary.DiaryResponse;
import org.springframework.stereotype.Component;

@Component
public class DiaryMapper {
    public DiaryResponse toResponse(Diary diary) {
        return DiaryResponse.builder()
                .id(diary.getId())
                .nickname(diary.getUser().getNickname())
                .title(diary.getTitle())
                .content(diary.getContent())
                .visibility(diary.getVisibility().toString())
                .weather(diary.getWeather())
                .commentsCount(diary.getComments() != null ? (long) diary.getComments().size() : null)
                .likesCount(diary.getLikes() != null ? (long) diary.getLikes().size() : null)
                .isLiked(false)
                .createdAt(diary.getCreatedAt().toString())
                .build();
    }
}
