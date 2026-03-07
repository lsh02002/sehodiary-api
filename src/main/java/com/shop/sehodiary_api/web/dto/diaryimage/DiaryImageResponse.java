package com.shop.sehodiary_api.web.dto.diaryimage;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaryImageResponse {
    private Long id;
    private Long diaryId;
    private Long uploaderId;
    private String fileName;
    private String fileUrl;
    private String mimeType;
    private Long sizeBytes;
    private Boolean deleted;
}
