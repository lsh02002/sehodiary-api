package com.shop.sehodiary_api.service.diaryimage;

import com.shop.sehodiary_api.repository.activity.ActivityAction;
import com.shop.sehodiary_api.repository.activity.ActivityEntityType;
import com.shop.sehodiary_api.repository.activity.function.SnapshotFunc;
import com.shop.sehodiary_api.repository.diary.Diary;
import com.shop.sehodiary_api.repository.diary.DiaryRepository;
import com.shop.sehodiary_api.repository.diaryImage.DiaryImage;
import com.shop.sehodiary_api.repository.diaryImage.DiaryImageRepository;
import com.shop.sehodiary_api.repository.user.User;
import com.shop.sehodiary_api.repository.user.UserRepository;
import com.shop.sehodiary_api.service.activelog.ActivityLogService;
import com.shop.sehodiary_api.service.exceptions.ConflictException;
import com.shop.sehodiary_api.service.exceptions.NotAcceptableException;
import com.shop.sehodiary_api.service.exceptions.NotFoundException;
import com.shop.sehodiary_api.service.s3.S3StorageService;
import com.shop.sehodiary_api.web.dto.diaryimage.DiaryImageResponse;
import com.shop.sehodiary_api.web.dto.diaryimage.FileRequest;
import com.shop.sehodiary_api.web.mapper.diaryimage.DiaryImageMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class DiaryImageService {
    private final UserRepository userRepository;
    private final DiaryRepository diaryRepository;
    private final DiaryImageRepository diaryImageRepository;
    private final DiaryImageMapper diaryImageMapper;
    private final ActivityLogService activityLogService;
    private final SnapshotFunc snapshotFunc;

    // 정책 값 (필요시 @Value로 주입)
    private static final long MAX_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_MIME_PREFIXES = List.of(
            "image/", "text/", "application/pdf", "application/zip"
    );
    private final S3StorageService s3storageService;

    public DiaryImageResponse uploadFile(User uploader, Diary diary, MultipartFile file) {
        validateFile(file);

        FileRequest stored = s3storageService.saveFile(file);

        DiaryImage savedDiaryImage = diaryImageRepository.save(
                DiaryImage.builder()
                        .diary(diary)
                        .uploader(uploader)
                        .imageUrl("/" + stored.storedKey())
                        .fileName(stored.originalFileName())
                        .mimeType(stored.mimeType())
                        .sizeBytes(stored.sizeBytes())
                        .deleted(false)
                        .build()
        );

        diary.getDiaryImages().add(savedDiaryImage);

        Object afterDiaryImage = snapshotFunc.snapshot(savedDiaryImage);

        activityLogService.log(ActivityEntityType.DIARY_IMAGE, ActivityAction.CREATE, savedDiaryImage.getId(), savedDiaryImage.logMessage(), uploader, null, afterDiaryImage);

        return diaryImageMapper.toResponse(savedDiaryImage);
    }

    public DiaryImageResponse uploadFileByUserIdAndDiaryId(Long userId, Long diaryId, MultipartFile file) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 글입니다. id=", diaryId));
        User uploader = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 사용자입니다. id=", userId));

        validateFile(file);

        FileRequest stored = s3storageService.saveFile(file);

        DiaryImage savedDiaryImage = diaryImageRepository.save(
                DiaryImage.builder()
                        .diary(diary)
                        .profileUser(null)
                        .uploader(uploader)
                        .imageUrl("/" + stored.storedKey())
                        .fileName(stored.originalFileName())
                        .mimeType(stored.mimeType())
                        .sizeBytes(stored.sizeBytes())
                        .deleted(false)
                        .build()
        );

        diary.getDiaryImages().add(savedDiaryImage);

        Object afterDiaryImage = snapshotFunc.snapshot(savedDiaryImage);

        activityLogService.log(ActivityEntityType.DIARY_IMAGE, ActivityAction.CREATE, savedDiaryImage.getId(), savedDiaryImage.logMessage(), uploader, null, afterDiaryImage);

        return diaryImageMapper.toResponse(savedDiaryImage);
    }

    public List<DiaryImageResponse> uploadManyFiles(Long userId, Long diaryId, List<MultipartFile> files) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 글입니다. id=", diaryId));
        User uploader = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 사용자입니다. id=", userId));

        List<DiaryImageResponse> responses = new ArrayList<>();

        List<DiaryImage> currentImages = diary.getDiaryImages().stream()
                .filter(image -> !image.getDeleted())
                .toList();

        if (files == null || files.isEmpty()) {
            List<DiaryImage> imagesToDelete = new ArrayList<>(uploader.getProfileImages());

            for (DiaryImage image : imagesToDelete) {
                deleteFile(uploader, image); // 실제 파일 삭제만
                diary.removeDiaryImage(image); // 연관관계 제거
            }
            return responses;
        }

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
                responses.add(uploadFile(uploader, diary, file));
            }
        }

        return responses;
    }

    public void deleteFile(User user, DiaryImage entity) {
        if(!entity.getDeleted()) {
            entity.setDeleted(true);
        }

        Object afterDiaryImage = snapshotFunc.snapshot(entity);

        activityLogService.log(ActivityEntityType.DIARY_IMAGE, ActivityAction.DELETE, entity.getId(), entity.logMessage(), user, null, afterDiaryImage);
    }

    public void deleteFileByUserIdAndDiaryImageId(Long userId, Long entityId) {
        DiaryImage entity = diaryImageRepository.findById(entityId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 글입니다. id=", entityId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 사용자입니다. id=", userId));

        Object beforeDiaryImage = snapshotFunc.snapshot(entity);

        // fileUrl에서 저장 파일명을 추출할 수 있게 해두었다면 여기서 삭제
        // 단순하게 storedFileName만 별도 칼럼으로 저장하는 방법을 권장합니다.
        // 예시로 fileUrl 마지막 토큰을 파일명으로 가정:
        String storedFileName = extractStoredFileName(entity.getImageUrl());
        // s3storageService.deleteFile(storedFileName);
        if(!entity.getDeleted()) {
            entity.setDeleted(true);
        }

        // diaryImageRepository.delete(entity);

        Object afterDiaryImage = snapshotFunc.snapshot(entity);

        activityLogService.log(ActivityEntityType.DIARY_IMAGE, ActivityAction.DELETE, entity.getId(), entity.logMessage(), user, beforeDiaryImage, afterDiaryImage);
    }

    public void deleteManyFiles(Diary diary) {
        for (DiaryImage image: diary.getDiaryImages()) {
            if(image.getDeleted() == true) {
                continue;
            }

            deleteFileByUserIdAndDiaryImageId(diary.getUser().getId(), image.getId());
        }
    }

    public DiaryImageResponse findDiaryImageId(Long diaryImageId) {
        DiaryImage entity = diaryImageRepository.findByIdAndDeletedNot(diaryImageId, true)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 첨부파일입니다. id=", diaryImageId));
        return diaryImageMapper.toResponse(entity);
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

    private String extractStoredFileName(String fileUrl) {
        if (fileUrl == null) return null;
        int idx = fileUrl.lastIndexOf('/');
        return (idx >= 0 && idx + 1 < fileUrl.length()) ? fileUrl.substring(idx + 1) : fileUrl;
    }
}
