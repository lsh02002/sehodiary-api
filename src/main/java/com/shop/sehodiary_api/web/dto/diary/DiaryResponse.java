package com.shop.sehodiary_api.web.dto.diary;

import com.shop.sehodiary_api.web.dto.diaryimage.DiaryImageResponse;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaryResponse {
    private Long id;
    private String nickname;
    private String title;
    private String content;
    private String visibility;
    private String weather;
    private Long commentsCount;
    private Long likesCount;
    private Boolean isLiked;
    private List<DiaryImageResponse> imageResponses;
    private String emoji;
    private String createdAt;
}
