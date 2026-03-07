package com.shop.sehodiary_api.web.mapper.diaryimage;

import com.shop.sehodiary_api.repository.diaryImage.DiaryImage;
import com.shop.sehodiary_api.web.dto.diaryimage.DiaryImageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DiaryImageMapper {
    @Value("${cloud.aws.s3.bucket}")
    private String bucket;
    @Value("${cloud.aws.region.static}")
    private String region;

    public DiaryImageResponse toResponse(DiaryImage diaryImage) {
        return DiaryImageResponse.builder()
                .id(diaryImage.getId())
                .diaryId(diaryImage.getDiary() != null ? diaryImage.getDiary().getId() : null)
                .uploaderId(diaryImage.getUploader() != null ? diaryImage.getUploader().getId() : null)
                .fileName(diaryImage.getFileName())
                .fileUrl("https://" + bucket + ".s3." + region + ".amazonaws.com" + diaryImage.getImageUrl())
                .deleted(diaryImage.getDeleted())
                .build();
    }
}