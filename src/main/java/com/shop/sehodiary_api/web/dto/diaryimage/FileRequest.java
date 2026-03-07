package com.shop.sehodiary_api.web.dto.diaryimage;

import lombok.Builder;

/**
 * @param storedFileName 물리 저장명 (UUID 등)
 */
@Builder
public record FileRequest(
        String originalFileName,
        String storedFileName,
        String storedKey,
        String mimeType,
        long sizeBytes
) {
}
