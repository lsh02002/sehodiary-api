package com.shop.sehodiary_api.service.profileimage;

import com.shop.sehodiary_api.repository.activity.ActivityAction;
import com.shop.sehodiary_api.repository.activity.ActivityEntityType;
import com.shop.sehodiary_api.repository.activity.function.SnapshotFunc;
import com.shop.sehodiary_api.repository.diaryImage.DiaryImage;
import com.shop.sehodiary_api.repository.diaryImage.DiaryImageRepository;
import com.shop.sehodiary_api.repository.user.User;
import com.shop.sehodiary_api.repository.user.UserRepository;
import com.shop.sehodiary_api.service.activelog.ActivityLogService;
import com.shop.sehodiary_api.service.s3.S3StorageService;
import com.shop.sehodiary_api.service.exceptions.ConflictException;
import com.shop.sehodiary_api.service.exceptions.NotAcceptableException;
import com.shop.sehodiary_api.service.exceptions.NotFoundException;
import com.shop.sehodiary_api.web.dto.diaryimage.FileRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class ProfileImageService {
    private final UserRepository userRepository;

    private final DiaryImageRepository diaryImageRepository;
    private final ActivityLogService activityLogService;
    private final SnapshotFunc snapshotFunc;

    // 정책 값 (필요시 @Value로 주입)
    private static final long MAX_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_MIME_PREFIXES = List.of(
            "image/", "text/", "application/pdf", "application/zip"
    );
    private final S3StorageService s3storageService;

    public void uploadFile(User uploader, MultipartFile file) {
        validateFile(file);

        FileRequest stored = s3storageService.saveFile(file);

        DiaryImage savedDiaryImage = diaryImageRepository.save(
                DiaryImage.builder()
                        .diary(null)
                        .profileUser(uploader)
                        .uploader(uploader)
                        .imageUrl("/" + stored.storedKey())
                        .fileName(stored.originalFileName())
                        .mimeType(stored.mimeType())
                        .sizeBytes(stored.sizeBytes())
                        .deleted(false)
                        .build()
        );

        uploader.getProfileImages().add(savedDiaryImage);

        Object afterDiaryImage = snapshotFunc.snapshot(savedDiaryImage);

        activityLogService.log(ActivityEntityType.DIARY_IMAGE, ActivityAction.CREATE, savedDiaryImage.getId(), savedDiaryImage.logMessage(), uploader, null, afterDiaryImage);
    }

    public void uploadManyFiles(Long userId, List<MultipartFile> files) {
        User uploader = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 사용자입니다. id=", userId));

        if (files == null || files.isEmpty()) {
            return;
        }

        List<DiaryImage> currentImages = uploader.getProfileImages().stream()
                .filter(image -> !image.getDeleted())
                .toList();

        for (DiaryImage diaryImage : currentImages) {
            boolean existsInRequest = files.stream().anyMatch(file ->
                    Objects.equals(diaryImage.getFileName(), file.getOriginalFilename())
                            && diaryImage.getSizeBytes() == file.getSize()
            );

            if (!existsInRequest) {
                deleteFile(uploader, diaryImage);
            }
        }

        for (MultipartFile file : files) {
            boolean existsInDb = currentImages.stream().anyMatch(image ->
                    Objects.equals(image.getFileName(), file.getOriginalFilename())
                            && image.getSizeBytes() == file.getSize()
            );

            if (!existsInDb) {
                uploadFile(uploader, file);
            }
        }
    }

    public void deleteFile(User user, DiaryImage entity) {

        if(!entity.getDeleted()) {
            entity.setDeleted(true);
        }

        Object beforeDiaryImage = snapshotFunc.snapshot(entity);

        activityLogService.log(ActivityEntityType.DIARY_IMAGE, ActivityAction.DELETE, entity.getId(), entity.logMessage(), user, beforeDiaryImage, null);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new NotAcceptableException("빈 파일은 업로드할 수 없습니다.", null);
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new NotAcceptableException("파일이 너무 큽니다. 최대 ", (MAX_SIZE_BYTES / (1024 * 1024)) + "MB");
        }
        String mime = file.getContentType();
        if (mime == null) {
            throw new ConflictException("지원하지 않는 파일 형식입니다. (MIME 미확인)", null);
        }
        boolean allowed = ALLOWED_MIME_PREFIXES.stream().anyMatch(prefix ->
                (prefix.endsWith("/") && mime.startsWith(prefix)) || mime.equals(prefix)
        );
        if (!allowed) {
            throw new ConflictException("지원하지 않는 파일 형식입니다. type=", mime);
        }
        // (선택) 실행파일/스크립트 차단: application/x-msdownload 등
        if ("application/octet-stream".equals(mime) && file.getOriginalFilename() != null) {
            String name = file.getOriginalFilename().toLowerCase();
            if (name.endsWith(".exe") || name.endsWith(".sh") || name.endsWith(".bat")) {
                throw new NotAcceptableException("실행 파일 업로드는 허용되지 않습니다.", null);
            }
        }
    }
}
