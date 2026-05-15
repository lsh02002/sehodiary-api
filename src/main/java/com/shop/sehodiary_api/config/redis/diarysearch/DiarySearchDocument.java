package com.shop.sehodiary_api.config.redis.diarysearch;

import com.redis.om.spring.annotations.Document;
import com.redis.om.spring.annotations.Indexed;
import com.redis.om.spring.annotations.Searchable;
import lombok.*;
import org.springframework.data.annotation.Id;

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
    private Long createdAt;
}
