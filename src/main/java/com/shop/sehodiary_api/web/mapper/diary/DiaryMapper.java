package com.shop.sehodiary_api.web.mapper.diary;

import com.shop.sehodiary_api.config.s3.S3Address;
import com.shop.sehodiary_api.repository.diary.Diary;
import com.shop.sehodiary_api.web.dto.diary.DiaryResponse;
import com.shop.sehodiary_api.web.mapper.diaryimage.DiaryImageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DiaryMapper {
    private final DiaryImageMapper diaryImageMapper;
    private final S3Address s3Address;

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
                .imageResponses(diary.getDiaryImages() != null ? diary.getDiaryImages().stream().filter(diaryImage -> !diaryImage.getDeleted()).map(diaryImageMapper::toResponse).toList() : null)
                .profileImage(
                        diary.getUser().getProfileImages() != null &&
                                !diary.getUser().getProfileImages().isEmpty()
                                ? s3Address.siteAddress() +
                                diary.getUser().getProfileImages()
                                        .get(diary.getUser().getProfileImages().size() - 1)
                                        .getImageUrl()
                                : null
                )
                .emoji(
                        Optional.ofNullable(diary.getDiaryEmotions())
                                .filter(list -> !list.isEmpty())
                                .map(list -> list.get(0))
                                .map(de -> de.getEmotion().getEmoji())
                                .orElse(null)
                )
                .createdAt(diary.getCreatedAt().toString())
                .build();
    }
}
