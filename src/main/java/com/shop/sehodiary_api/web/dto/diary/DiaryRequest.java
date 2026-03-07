package com.shop.sehodiary_api.web.dto.diary;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaryRequest {
    private String title;
    private String content;
    private String visibility;
    private String weather;
    private String emoji;
}
