package com.shop.sehodiary_api.web.mapper.diaryimage;

import com.shop.sehodiary_api.config.s3.S3Address;
import com.shop.sehodiary_api.repository.diaryImage.DiaryImage;
import com.shop.sehodiary_api.web.dto.diaryimage.DiaryImageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DiaryImageMapper {
    private final S3Address s3Address;

    public DiaryImageResponse toResponse(DiaryImage diaryImage) {
        return DiaryImageResponse.builder()
                .id(diaryImage.getId())
                .diaryId(diaryImage.getDiary() != null ? diaryImage.getDiary().getId() : null)
                .uploaderId(diaryImage.getUploader() != null ? diaryImage.getUploader().getId() : null)
                .fileName(diaryImage.getFileName())
                .fileUrl(s3Address.siteAddress() + diaryImage.getImageUrl())
                .deleted(diaryImage.getDeleted())
                .build();
    }
}