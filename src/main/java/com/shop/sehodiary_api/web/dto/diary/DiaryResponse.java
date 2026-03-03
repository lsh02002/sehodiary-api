package com.shop.sehodiary_api.web.dto.diary;

import lombok.*;

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
    private String createdAt;
}
