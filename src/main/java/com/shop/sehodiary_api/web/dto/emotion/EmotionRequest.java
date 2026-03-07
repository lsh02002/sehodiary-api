package com.shop.sehodiary_api.web.dto.emotion;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmotionRequest {
    private String name;
    private String emoji;
}
