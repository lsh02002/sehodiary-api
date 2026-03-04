package com.shop.sehodiary_api.repository.like;

import com.shop.sehodiary_api.repository.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {
    Optional<Like> findByUserIdAndDiaryId(Long userId, Long diaryId);
    Boolean existsByUserIdAndDiaryId(Long userId, Long diaryId);
    List<Like> findAllByUserId(Long userId);
    List<Like> findByDiaryId(Long diaryId);
}
