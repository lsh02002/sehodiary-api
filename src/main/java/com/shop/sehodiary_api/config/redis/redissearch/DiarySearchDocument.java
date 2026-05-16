package com.shop.sehodiary_api.config.redis.redissearch;

import com.redis.om.spring.annotations.Document;
import com.redis.om.spring.annotations.Indexed;
import com.redis.om.spring.annotations.Searchable;
import com.shop.sehodiary_api.repository.diary.Diary;
import lombok.*;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

@Document("diary-search")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiarySearchDocument {

    @Id
    private String id; // diaryId 문자열로 저장

    @Indexed
    private Long diaryId;

    @Indexed
    private Long userId;

    @Searchable
    private String title;

    @Searchable
    private String content;

    @Indexed
    private String visibility;

    @Indexed
    private LocalDateTime createdAt;

    public static DiarySearchDocument from(Diary diary) {
        return DiarySearchDocument.builder()
                .diaryId(diary.getId())
                .userId(diary.getUser().getId())
                .title(diary.getTitle())
                .content(diary.getContent())
                .visibility(diary.getVisibility().name())
                .createdAt(diary.getCreatedAt())
                .build();
    }
}