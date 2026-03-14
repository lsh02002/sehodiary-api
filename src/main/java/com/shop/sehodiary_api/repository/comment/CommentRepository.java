package com.shop.sehodiary_api.repository.comment;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Optional<Comment> findByUserIdAndId(Long userId, Long commentId);
    List<Comment> findByDiaryId(Long diaryId);
    List<Comment> findByUserId(Long userId);

    @Query("select c.id from Comment c where c.diary.id = :diaryId")
    List<Long> findAllIdsByDiaryId(@Param("diaryId") Long diaryId);

    @Query("select c.id from Comment c where c.user.id = :userId")
    List<Long> findAllIdsByUserId(@Param("userId") Long userId);

    @Query("select d.id from Comment d where d.user.id = :userId")
    List<Long> findIdsByUserId(Long userId);

    @Query("""
    select c
    from Comment c
    join fetch c.user u
    join fetch c.diary d
    where u.id = :userId
""")
    List<Comment> findAllByUserIdWithUser(@Param("userId") Long userId);
}
