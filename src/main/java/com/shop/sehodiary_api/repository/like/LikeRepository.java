package com.shop.sehodiary_api.repository.like;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {
    Optional<Like> findByUserIdAndDiaryId(Long userId, Long diaryId);
    Boolean existsByUserIdAndDiaryId(Long userId, Long diaryId);
    List<Like> findAllByUserId(Long userId);
    List<Like> findByDiaryId(Long diaryId);


    @Query("""
    select l.diary.id
    from Like l
    where l.user.id = :userId
      and l.diary.id in :diaryIds
""")
    List<Long> findLikedDiaryIds(@Param("userId") Long userId,
                                 @Param("diaryIds") List<Long> diaryIds);
}
