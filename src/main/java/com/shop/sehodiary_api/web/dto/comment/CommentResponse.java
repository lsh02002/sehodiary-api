package com.shop.sehodiary_api.web.dto.comment;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse implements Serializable {
    private Long diaryId;
    private Long commentId;
    private String nickname;
    private String profileImage;
    private String content;
    private String createdAt;
    private String updatedAt;
}
