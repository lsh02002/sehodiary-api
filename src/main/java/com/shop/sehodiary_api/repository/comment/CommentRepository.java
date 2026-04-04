package com.shop.sehodiary_api.repository.comment;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Optional<Comment> findByUserIdAndId(Long userId, Long commentId);

    @Query("select c.id from Comment c where c.diary.id = :diaryId order by c.id desc")
    List<Long> findAllIdsByDiaryIdDesc(@Param("diaryId") Long diaryId);

    @Query("select c.id from Comment c where c.user.id = :userId order by c.id desc")
    List<Long> findAllIdsByUserIdDesc(@Param("userId") Long userId);
}
