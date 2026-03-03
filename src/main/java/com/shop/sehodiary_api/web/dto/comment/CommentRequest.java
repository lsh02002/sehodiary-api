package com.shop.sehodiary_api.web.dto.comment;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CommentRequest {
    private Long diaryId;
    private String content;
}
