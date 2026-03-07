package com.shop.sehodiary_api.repository.diaryImage;

import com.shop.sehodiary_api.repository.activity.logger.Loggable;
import com.shop.sehodiary_api.repository.common.BaseTimeEntity;
import com.shop.sehodiary_api.repository.diary.Diary;
import com.shop.sehodiary_api.repository.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "diary_images")
public class DiaryImage extends BaseTimeEntity implements Loggable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "diary_id", nullable = false)
    private Diary diary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;

    @Column(name = "image_url", nullable = false, length = 2048)
    private String imageUrl;

    @Column(name = "file_name", length = 512)
    private String fileName;

    @Column(name = "mime_type", length = 255)
    private String mimeType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column
    private Boolean deleted;

    public DiaryImage(Diary diary, String imageUrl, String fileName, String mimeType, Long sizeBytes) {
        this.diary = diary;
        this.imageUrl = imageUrl;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
    }

    @Override
    public String logMessage() {
        return "글 '" + diary.getTitle() + "'의 IMAGE 파일 '" + fileName + "' ";
    }
}
