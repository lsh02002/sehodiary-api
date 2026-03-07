package com.shop.sehodiary_api.web.mapper.comment;

import com.shop.sehodiary_api.repository.comment.Comment;
import com.shop.sehodiary_api.web.dto.comment.CommentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CommentMapper {
    @Value("${cloud.aws.s3.bucket}")
    private String bucket;
    @Value("${cloud.aws.region.static}")
    private String region;

    public CommentResponse toResponse(Comment comment) {
        return CommentResponse.builder()
                .diaryId(comment.getDiary().getId())
                .commentId(comment.getId())
                .nickname(comment.getUser().getNickname())
                .profileImage(
                        comment.getUser().getProfileImages() != null &&
                                !comment.getUser().getProfileImages().isEmpty()
                                ? "https://" + bucket + ".s3." + region + ".amazonaws.com" +
                                comment.getUser().getProfileImages().get(0).getImageUrl()
                                : null
                )
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt().toString())
                .updatedAt(comment.getUpdatedAt().toString())
                .build();
    }
}
