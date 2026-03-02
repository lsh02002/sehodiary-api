package com.shop.sehodiary_api.repository.diary;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DiaryRepository extends JpaRepository<Diary, Long> {
    Optional<Diary> findByUserIdAndId(Long userId, Long diaryId);
    void deleteByUserIdAndId(Long userId, Long diaryId);
    List<Diary> findByUserId(Long userId);
}
