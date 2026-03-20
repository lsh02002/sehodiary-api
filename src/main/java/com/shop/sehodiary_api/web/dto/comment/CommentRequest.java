package com.shop.sehodiary_api.web.dto.comment;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentRequest {
    private Long diaryId;
    private String content;
}
