package com.shop.sehodiary_api.repository.emotion;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmotionRepository extends JpaRepository<Emotion, Long> {
    boolean existsByName(String name);
    boolean existsByEmoji(String emoji);
}
