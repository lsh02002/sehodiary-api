package com.shop.sehodiary_api.repository.diaryEmotion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DiaryEmotionRepository extends JpaRepository<DiaryEmotion, Long> {
    List<DiaryEmotion> findByDiaryId(Long diaryId);
    Optional<DiaryEmotion> findByDiaryIdAndEmotionName(Long diaryId, String emotionName);
}
