package com.shop.sehodiary_api.web.dto.diaryemotion;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaryEmotionResponse {
    private Long id;
    private Long diaryId;
    private String nickname;
    private String name;
    private String emoji;
    private String createdAt;
    private String updatedAt;
}
