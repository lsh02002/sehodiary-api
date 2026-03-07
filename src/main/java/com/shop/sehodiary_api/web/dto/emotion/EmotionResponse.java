package com.shop.sehodiary_api.web.dto.emotion;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmotionResponse {
    private Long id;
    private String name;
    private String emoji;
    private String createdAt;
    private String updatedAt;
}
