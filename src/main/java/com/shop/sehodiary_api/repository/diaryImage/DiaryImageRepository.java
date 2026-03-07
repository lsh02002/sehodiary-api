package com.shop.sehodiary_api.repository.diaryImage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DiaryImageRepository extends JpaRepository<DiaryImage, Long> {
    Optional<DiaryImage> findByUploaderIdAndId(Long userId, Long diaryImageId);
    Optional<DiaryImage> findByIdAndDeletedNot(Long diaryImageId, Boolean deleted);
}
