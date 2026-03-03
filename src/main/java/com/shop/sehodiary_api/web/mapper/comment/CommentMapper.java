package com.shop.sehodiary_api.web.mapper.comment;

import com.shop.sehodiary_api.repository.comment.Comment;
import com.shop.sehodiary_api.web.dto.comment.CommentResponse;
import org.springframework.stereotype.Component;

@Component
public class CommentMapper {
    public CommentResponse toResponse(Comment comment) {
        return CommentResponse.builder()
                .diaryId(comment.getDiary().getId())
                .commentId(comment.getId())
                .nickname(comment.getUser().getNickname())
                .profileImage(comment.getUser().getProfileImage())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt().toString())
                .updatedAt(comment.getUpdatedAt().toString())
                .build();
    }
}
