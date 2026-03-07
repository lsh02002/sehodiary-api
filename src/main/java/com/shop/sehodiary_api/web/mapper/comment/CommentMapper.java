package com.shop.sehodiary_api.web.mapper.comment;

import com.shop.sehodiary_api.config.s3.S3Address;
import com.shop.sehodiary_api.repository.comment.Comment;
import com.shop.sehodiary_api.web.dto.comment.CommentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentMapper {
    private final S3Address s3Address;

    public CommentResponse toResponse(Comment comment) {
        return CommentResponse.builder()
                .diaryId(comment.getDiary().getId())
                .commentId(comment.getId())
                .nickname(comment.getUser().getNickname())
                .profileImage(
                        comment.getUser().getProfileImages() != null &&
                                !comment.getUser().getProfileImages().isEmpty()
                                ? s3Address.siteAddress() +
                                comment.getUser().getProfileImages().get(0).getImageUrl()
                                : null
                )
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt().toString())
                .updatedAt(comment.getUpdatedAt().toString())
                .build();
    }
}
