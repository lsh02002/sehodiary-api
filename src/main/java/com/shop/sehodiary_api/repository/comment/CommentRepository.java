package com.shop.sehodiary_api.repository.comment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Optional<Comment> findByUserIdAndId(Long userId, Long commentId);
    List<Comment> findByDiaryId(Long diaryId);
}
