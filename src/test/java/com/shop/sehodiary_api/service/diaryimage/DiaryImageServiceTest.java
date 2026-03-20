package com.shop.sehodiary_api.service.diaryimage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class DiaryImageServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private DiaryRepository diaryRepository;
    @Mock
    private DiaryImageRepository diaryImageRepository;
    @Mock
    private DiaryImageMapper diaryImageMapper;
    @Mock
    private ActivityLogService activityLogService;
    @Mock
    private SnapshotFunc snapshotFunc;
    @Mock
    private S3StorageService s3storageService;

    @InjectMocks
    private DiaryImageService diaryImageService;

    private User mockUser() {
        return mock(User.class);
    }

    private Diary mockDiary() {
        return mock(Diary.class);
    }

    private DiaryImage mockDiaryImage() {
        return mock(DiaryImage.class);
    }

    private MockMultipartFile imageFile(String name, long size) {
        byte[] content = new byte[(int) size];
        return new MockMultipartFile("file", name, "image/png", content);
    }

    @Nested
    @DisplayName("uploadFile()")
    class UploadFileTest {

        @Test
        @DisplayName("정상 파일이면 저장 후 response를 반환한다")
        void uploadSuccess() {
            User uploader = mockUser();
            Diary diary = mockDiary();
            MockMultipartFile file = imageFile("a.png", 100L);

            List<DiaryImage> diaryImages = new LinkedList<>();
            Map<String, Object> snapshot = new HashMap<>();

            FileRequest stored = new FileRequest(
                    "a.png",
                    "stored-a.png",
                    "stored-a-key",
                    "image/png",
                    100L
            );

            DiaryImage savedImage = mock(DiaryImage.class);
            DiaryImageResponse response = mock(DiaryImageResponse.class);

            when(savedImage.getId()).thenReturn(10L);
            when(savedImage.logMessage()).thenReturn("log-message");
            when(diary.getDiaryImages()).thenReturn(diaryImages);

            when(s3storageService.saveFile(any(MultipartFile.class))).thenReturn(stored);
            when(diaryImageRepository.save(any(DiaryImage.class))).thenReturn(savedImage);
            when(snapshotFunc.snapshot(any(DiaryImage.class))).thenReturn(snapshot);
            when(diaryImageMapper.toResponse(savedImage)).thenReturn(response);

            DiaryImageResponse result = diaryImageService.uploadFile(uploader, diary, file);

            assertThat(result).isSameAs(response);
            assertThat(diaryImages).contains(savedImage);

            verify(s3storageService).saveFile(any(MultipartFile.class));
            verify(diaryImageRepository).save(any(DiaryImage.class));
            verify(diaryImageMapper).toResponse(savedImage);
            verify(activityLogService).log(
                    eq(ActivityEntityType.DIARY_IMAGE),
                    eq(ActivityAction.CREATE),
                    eq(10L),
                    eq("log-message"),
                    eq(uploader),
                    isNull(),
                    eq(snapshot)
            );
        }

        @Test
        @DisplayName("빈 파일이면 예외가 발생한다")
        void throwWhenFileEmpty() {
            User uploader = mockUser();
            Diary diary = mockDiary();
            MultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

            assertThatThrownBy(() -> diaryImageService.uploadFile(uploader, diary, file))
                    .isInstanceOf(NotAcceptableException.class);

            verifyNoInteractions(s3storageService, diaryImageRepository, diaryImageMapper, activityLogService);
        }

        @Test
        @DisplayName("크기가 10MB 초과면 예외가 발생한다")
        void throwWhenFileTooLarge() {
            User uploader = mockUser();
            Diary diary = mockDiary();
            MultipartFile file = imageFile("big.png", 10 * 1024 * 1024 + 1L);

            assertThatThrownBy(() -> diaryImageService.uploadFile(uploader, diary, file))
                    .isInstanceOf(NotAcceptableException.class);

            verifyNoInteractions(s3storageService, diaryImageRepository, diaryImageMapper, activityLogService);
        }

        @Test
        @DisplayName("mime type이 null이면 예외가 발생한다")
        void throwWhenMimeNull() {
            User uploader = mockUser();
            Diary diary = mockDiary();
            MultipartFile file = new MockMultipartFile("file", "a.bin", null, new byte[]{1, 2, 3});

            assertThatThrownBy(() -> diaryImageService.uploadFile(uploader, diary, file))
                    .isInstanceOf(ConflictException.class);

            verifyNoInteractions(s3storageService, diaryImageRepository, diaryImageMapper, activityLogService);
        }

        @Test
        @DisplayName("허용되지 않은 mime type이면 예외가 발생한다")
        void throwWhenMimeNotAllowed() {
            User uploader = mockUser();
            Diary diary = mockDiary();
            MultipartFile file = new MockMultipartFile("file", "a.json", "application/json", new byte[]{1, 2, 3});

            assertThatThrownBy(() -> diaryImageService.uploadFile(uploader, diary, file))
                    .isInstanceOf(ConflictException.class);

            verifyNoInteractions(s3storageService, diaryImageRepository, diaryImageMapper, activityLogService);
        }
    }

    @Nested
    @DisplayName("uploadFileByUserIdAndDiaryId()")
    class UploadFileByUserIdAndDiaryIdTest {

        @Test
        @DisplayName("정상 파일이면 업로드 성공")
        void uploadSuccess() {
            Long userId = 1L;
            Long diaryId = 2L;

            User user = mockUser();
            Diary diary = mockDiary();
            MultipartFile file = imageFile("a.png", 100L);

            List<DiaryImage> diaryImages = new LinkedList<>();
            FileRequest stored = new FileRequest(
                    "a.png",
                    "stored-a.png",
                    "stored-a-key",
                    "image/png",
                    100L
            );

            DiaryImage savedImage = mock(DiaryImage.class);
            DiaryImageResponse response = mock(DiaryImageResponse.class);
            Map<String, Object> snapshot = new HashMap<>();

            when(diary.getDiaryImages()).thenReturn(diaryImages);

            when(savedImage.getId()).thenReturn(10L);
            when(savedImage.logMessage()).thenReturn("log-message");

            when(diaryRepository.findById(diaryId)).thenReturn(Optional.of(diary));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(s3storageService.saveFile(any(MultipartFile.class))).thenReturn(stored);
            when(diaryImageRepository.save(any(DiaryImage.class))).thenReturn(savedImage);
            when(snapshotFunc.snapshot(savedImage)).thenReturn(snapshot);
            when(diaryImageMapper.toResponse(savedImage)).thenReturn(response);

            DiaryImageResponse result = diaryImageService.uploadFileByUserIdAndDiaryId(userId, diaryId, file);

            assertThat(diaryImages).contains(savedImage);
            assertThat(result).isEqualTo(response);

            verify(s3storageService).saveFile(any(MultipartFile.class));
            verify(diaryImageRepository).save(any(DiaryImage.class));
            verify(diaryImageMapper).toResponse(savedImage);
        }

        @Test
        @DisplayName("diary가 없으면 예외가 발생한다")
        void throwWhenDiaryNotFound() {
            when(diaryRepository.findById(2L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> diaryImageService.uploadFileByUserIdAndDiaryId(1L, 2L, imageFile("a.png", 10L)))
                    .isInstanceOf(NotFoundException.class);

            verify(userRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("user가 없으면 예외가 발생한다")
        void throwWhenUserNotFound() {
            Diary diary = mockDiary();
            when(diaryRepository.findById(2L)).thenReturn(Optional.of(diary));
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> diaryImageService.uploadFileByUserIdAndDiaryId(1L, 2L, imageFile("a.png", 10L)))
                    .isInstanceOf(NotFoundException.class);

            verifyNoInteractions(s3storageService, diaryImageRepository, diaryImageMapper, activityLogService);
        }
    }

    @Nested
    @DisplayName("uploadManyFiles()")
    class UploadManyFilesTest {

        @Test
        @DisplayName("files가 null이면 빈 리스트를 반환한다")
        void returnEmptyWhenFilesNull() {
            Long userId = 1L;
            Long diaryId = 2L;
            Diary diary = mockDiary();
            User user = mockUser();

            when(diaryRepository.findById(diaryId)).thenReturn(Optional.of(diary));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            List<DiaryImageResponse> result = diaryImageService.uploadManyFiles(userId, diaryId, null);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("files가 비어있으면 빈 리스트를 반환한다")
        void returnEmptyWhenFilesEmpty() {
            Long userId = 1L;
            Long diaryId = 2L;
            Diary diary = mockDiary();
            User user = mockUser();

            when(diaryRepository.findById(diaryId)).thenReturn(Optional.of(diary));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            List<DiaryImageResponse> result = diaryImageService.uploadManyFiles(userId, diaryId, List.of());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("요청에 없는 기존 파일은 삭제 처리한다")
        void deleteImagesNotIncludedInRequest() {
            Long userId = 1L;
            Long diaryId = 2L;
            User user = mockUser();
            Diary diary = mock(Diary.class);

            DiaryImage keepImage = mockDiaryImage();
            DiaryImage deleteImage = mockDiaryImage();

            MockMultipartFile keepFile = imageFile("keep.png", 100L);

            Map<String, Object> beforeSnapshot = new HashMap<>();
            Map<String, Object> afterSnapshot = new HashMap<>();

            when(keepImage.getFileName()).thenReturn("keep.png");
            when(keepImage.getSizeBytes()).thenReturn(100L);
            when(keepImage.getDeleted()).thenReturn(false);

            when(deleteImage.getId()).thenReturn(20L);
            when(deleteImage.getFileName()).thenReturn("delete.png");
            when(deleteImage.getDeleted()).thenReturn(false);
            when(deleteImage.logMessage()).thenReturn("log-message");

            when(diaryRepository.findById(diaryId)).thenReturn(Optional.of(diary));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(diary.getDiaryImages()).thenReturn(List.of(keepImage, deleteImage));

            when(snapshotFunc.snapshot(any(DiaryImage.class))).thenReturn(beforeSnapshot).thenReturn(afterSnapshot);

            List<DiaryImageResponse> result =
                    diaryImageService.uploadManyFiles(userId, diaryId, List.of(keepFile));

            assertThat(result).isEmpty();

            verify(diaryImageRepository, never()).findById(10L);
            verify(deleteImage).setDeleted(true);

            verify(activityLogService).log(
                    eq(ActivityEntityType.DIARY_IMAGE),
                    eq(ActivityAction.DELETE),
                    eq(20L),
                    eq("log-message"),
                    eq(user),
                    eq(beforeSnapshot),
                    eq(afterSnapshot)
            );
        }

        @Test
        @DisplayName("DB에 없는 신규 파일만 업로드한다")
        void uploadOnlyNewFiles() {
            Long userId = 1L;
            Long diaryId = 2L;
            User user = mockUser();
            Diary diary = mockDiary();

            DiaryImage existingImage = mockDiaryImage();
            when(existingImage.getFileName()).thenReturn("keep.png");
            when(existingImage.getSizeBytes()).thenReturn(100L);
            when(existingImage.getDeleted()).thenReturn(false);

            when(diary.getDiaryImages()).thenReturn(new ArrayList<>(List.of(existingImage)));

            MockMultipartFile existingFile = imageFile("keep.png", 100L);
            MockMultipartFile newFile = imageFile("new.png", 120L);

            FileRequest stored = new FileRequest(
                    "new.png",
                    "stored-new.png",
                    "stored-new-key",
                    "image/png",
                    120L
            );
            DiaryImage savedImage = mock(DiaryImage.class);
            DiaryImageResponse response = mock(DiaryImageResponse.class);

            when(savedImage.getId()).thenReturn(30L);
            when(savedImage.logMessage()).thenReturn("log-message");

            when(diaryRepository.findById(diaryId)).thenReturn(Optional.of(diary));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(s3storageService.saveFile(any(MultipartFile.class))).thenReturn(stored);
            when(diaryImageRepository.save(any(DiaryImage.class))).thenReturn(savedImage);
            when(snapshotFunc.snapshot(any(DiaryImage.class))).thenReturn(new HashMap<>());
            when(diaryImageMapper.toResponse(savedImage)).thenReturn(response);

            List<DiaryImageResponse> result =
                    diaryImageService.uploadManyFiles(userId, diaryId, List.of(existingFile, newFile));

            assertThat(result).containsExactly(response);
            verify(s3storageService, times(1)).saveFile(any(MultipartFile.class));
            verify(diaryImageRepository, times(1)).save(any(DiaryImage.class));
            verify(diaryImageMapper, times(1)).toResponse(savedImage);
        }

        @Test
        @DisplayName("diary가 없으면 예외가 발생한다")
        void throwWhenDiaryNotFound() {
            when(diaryRepository.findById(2L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> diaryImageService.uploadManyFiles(1L, 2L, List.of(imageFile("a.png", 10L))))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("user가 없으면 예외가 발생한다")
        void throwWhenUserNotFound() {
            Diary diary = mockDiary();
            when(diaryRepository.findById(2L)).thenReturn(Optional.of(diary));
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> diaryImageService.uploadManyFiles(1L, 2L, List.of(imageFile("a.png", 10L))))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteFile()")
    class DeleteFileTest {

        @Test
        @DisplayName("삭제되지 않은 파일이면 deleted=true로 바꾸고 로그를 남긴다")
        void deleteSuccess() {
            User user = mockUser();
            DiaryImage image = mockDiaryImage();

            when(image.getId()).thenReturn(10L);
            when(image.logMessage()).thenReturn("log-message");

            when(snapshotFunc.snapshot(image)).thenReturn(new HashMap<>());

            diaryImageService.deleteFile(user, image);

            verify(image).setDeleted(true);
            verify(activityLogService).log(
                    eq(ActivityEntityType.DIARY_IMAGE),
                    eq(ActivityAction.DELETE),
                    eq(10L),
                    eq("log-message"),
                    eq(user),
                    eq(new HashMap<>()),
                    eq(new HashMap<>())
            );
        }

        @Test
        @DisplayName("이미 삭제된 파일이어도 로그는 남긴다")
        void logEvenAlreadyDeleted() {
            User user = mockUser();
            DiaryImage image = mockDiaryImage();

            when(image.getId()).thenReturn(10L);
            when(image.logMessage()).thenReturn("log-message");

            when(snapshotFunc.snapshot(image)).thenReturn(new HashMap<>());

            diaryImageService.deleteFile(user, image);

            verify(image).setDeleted(true);
            verify(activityLogService).log(
                    eq(ActivityEntityType.DIARY_IMAGE),
                    eq(ActivityAction.DELETE),
                    eq(10L),
                    eq("log-message"),
                    eq(user),
                    eq(new HashMap<>()),
                    eq(new HashMap<>())
            );
        }
    }

    @Nested
    @DisplayName("deleteFileByUserIdAndDiaryImageId()")
    class DeleteFileByUserIdAndDiaryImageIdTest {

        @Test
        @DisplayName("정상 삭제 시 before/after snapshot으로 로그를 남긴다")
        void deleteSuccess() {
            Long userId = 1L;
            Long imageId = 2L;

            User user = mockUser();
            DiaryImage image = mockDiaryImage();
            when(image.getId()).thenReturn(2L);
            when(image.logMessage()).thenReturn("log-message");
            when(image.getDeleted()).thenReturn(false);

            when(diaryImageRepository.findById(imageId)).thenReturn(Optional.of(image));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(snapshotFunc.snapshot(image)).thenReturn(new HashMap<>()).thenReturn(new HashMap<>());

            diaryImageService.deleteFileByUserIdAndDiaryImageId(userId, imageId);

            verify(image).setDeleted(true);
            verify(activityLogService).log(
                    eq(ActivityEntityType.DIARY_IMAGE),
                    eq(ActivityAction.DELETE),
                    eq(imageId),
                    eq("log-message"),
                    eq(user),
                    eq(new HashMap<>()),
                    eq(new HashMap<>())
            );
        }

        @Test
        @DisplayName("이미 삭제된 파일이면 setDeleted는 호출하지 않는다")
        void doNotSetDeletedWhenAlreadyDeleted() {
            Long userId = 1L;
            Long imageId = 2L;

            User user = mockUser();
            DiaryImage image = mockDiaryImage();

            when(image.getId()).thenReturn(2L);
            when(image.logMessage()).thenReturn("log-message");

            when(diaryImageRepository.findById(imageId)).thenReturn(Optional.of(image));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(snapshotFunc.snapshot(image)).thenReturn(new HashMap<>()).thenReturn(new HashMap<>());
            diaryImageService.deleteFileByUserIdAndDiaryImageId(userId, imageId);

            verify(image).setDeleted(true);
            verify(activityLogService).log(
                    eq(ActivityEntityType.DIARY_IMAGE),
                    eq(ActivityAction.DELETE),
                    eq(imageId),
                    eq("log-message"),
                    eq(user),
                    eq(new HashMap<>()),
                    eq(new HashMap<>())
            );
        }

        @Test
        @DisplayName("파일이 없으면 예외가 발생한다")
        void throwWhenImageNotFound() {
            when(diaryImageRepository.findById(2L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> diaryImageService.deleteFileByUserIdAndDiaryImageId(1L, 2L))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("유저가 없으면 예외가 발생한다")
        void throwWhenUserNotFound() {
            DiaryImage image = mockDiaryImage();
            when(diaryImageRepository.findById(2L)).thenReturn(Optional.of(image));
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> diaryImageService.deleteFileByUserIdAndDiaryImageId(1L, 2L))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteManyFiles()")
    class DeleteManyFilesTest {

        @Test
        @DisplayName("삭제되지 않은 파일들만 삭제한다")
        void deleteOnlyNotDeletedImages() {
            User user = mockUser();
            Diary diary = mock(Diary.class);

            DiaryImage active1 = mockDiaryImage();
            DiaryImage deleted = mockDiaryImage();
            DiaryImage active2 = mockDiaryImage();

            when(user.getId()).thenReturn(1L);

            when(active1.getId()).thenReturn(10L);
            when(active2.getId()).thenReturn(30L);

            when(active1.getDeleted()).thenReturn(false);
            when(deleted.getDeleted()).thenReturn(true);
            when(active2.getDeleted()).thenReturn(false);

            when(diary.getUser()).thenReturn(user);
            when(diary.getDiaryImages()).thenReturn(List.of(active1, deleted, active2));

            when(diaryImageRepository.findById(10L)).thenReturn(Optional.of(active1));
            when(diaryImageRepository.findById(30L)).thenReturn(Optional.of(active2));
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(snapshotFunc.snapshot(active1)).thenReturn(new HashMap<>()).thenReturn(new HashMap<>());
            when(snapshotFunc.snapshot(active2)).thenReturn(new HashMap<>()).thenReturn(new HashMap<>());

            diaryImageService.deleteManyFiles(diary);

            verify(diaryImageRepository).findById(10L);
            verify(diaryImageRepository, never()).findById(20L);
            verify(diaryImageRepository).findById(30L);
        }
    }

    @Nested
    @DisplayName("findDiaryImageId()")
    class FindDiaryImageIdTest {

        @Test
        @DisplayName("삭제되지 않은 첨부파일이면 response를 반환한다")
        void findSuccess() {
            DiaryImage image = mockDiaryImage();
            DiaryImageResponse response = mock(DiaryImageResponse.class);

            when(diaryImageRepository.findByIdAndDeletedNot(10L, true)).thenReturn(Optional.of(image));
            when(diaryImageMapper.toResponse(image)).thenReturn(response);

            DiaryImageResponse result = diaryImageService.findDiaryImageId(10L);

            assertThat(result).isSameAs(response);
        }

        @Test
        @DisplayName("삭제되었거나 없으면 예외가 발생한다")
        void throwWhenNotFound() {
            when(diaryImageRepository.findByIdAndDeletedNot(10L, true)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> diaryImageService.findDiaryImageId(10L))
                    .isInstanceOf(NotFoundException.class);
        }
    }
}