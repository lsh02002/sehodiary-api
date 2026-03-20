package com.shop.sehodiary_api.service.profileimage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.*;

import com.shop.sehodiary_api.repository.activity.ActivityAction;
import com.shop.sehodiary_api.repository.activity.ActivityEntityType;
import com.shop.sehodiary_api.repository.activity.function.SnapshotFunc;
import com.shop.sehodiary_api.repository.diaryImage.DiaryImage;
import com.shop.sehodiary_api.repository.diaryImage.DiaryImageRepository;
import com.shop.sehodiary_api.repository.user.User;
import com.shop.sehodiary_api.repository.user.UserRepository;
import com.shop.sehodiary_api.service.activelog.ActivityLogService;
import com.shop.sehodiary_api.service.exceptions.ConflictException;
import com.shop.sehodiary_api.service.exceptions.NotAcceptableException;
import com.shop.sehodiary_api.service.exceptions.NotFoundException;
import com.shop.sehodiary_api.service.s3.S3StorageService;
import com.shop.sehodiary_api.web.dto.diaryimage.FileRequest;
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
class ProfileImageServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private DiaryImageRepository diaryImageRepository;
    @Mock
    private ActivityLogService activityLogService;
    @Mock
    private SnapshotFunc snapshotFunc;
    @Mock
    private S3StorageService s3storageService;

    @InjectMocks
    private ProfileImageService profileImageService;

    private User mockUser() {
        return mock(User.class);
    }

    private DiaryImage mockDiaryImage() {
        return mock(DiaryImage.class);
    }

    private MockMultipartFile file(String name, String contentType, int size) {
        return new MockMultipartFile("file", name, contentType, new byte[size]);
    }

    @Nested
    @DisplayName("uploadFile()")
    class UploadFileTest {

        @Test
        @DisplayName("정상 파일이면 업로드 후 DiaryImage를 저장하고 로그를 남긴다")
        void uploadSuccess() {
            User uploader = mockUser();
            MultipartFile file = file("profile.png", "image/png", 100);

            FileRequest stored = new FileRequest(
                    "profile.png",
                    "stored-profile.png",
                    "profiles/stored-profile.png",
                    "image/png",
                    100L
            );

            DiaryImage savedDiaryImage = mock(DiaryImage.class);
            Map<String, Object> snapshot = new HashMap<>();

            when(savedDiaryImage.getId()).thenReturn(10L);
            when(savedDiaryImage.logMessage()).thenReturn("log-message");

            when(s3storageService.saveFile(file)).thenReturn(stored);
            when(diaryImageRepository.save(any(DiaryImage.class))).thenReturn(savedDiaryImage);
            when(snapshotFunc.snapshot(savedDiaryImage)).thenReturn(snapshot);

            profileImageService.uploadFile(uploader, file);

            verify(s3storageService).saveFile(file);
            verify(diaryImageRepository).save(any(DiaryImage.class));
            verify(snapshotFunc).snapshot(savedDiaryImage);
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
        @DisplayName("파일이 null이면 NotAcceptableException이 발생한다")
        void throwWhenFileNull() {
            User uploader = mockUser();

            assertThatThrownBy(() -> profileImageService.uploadFile(uploader, null))
                    .isInstanceOf(NotAcceptableException.class);

            verifyNoInteractions(s3storageService, diaryImageRepository, activityLogService, snapshotFunc);
        }

        @Test
        @DisplayName("빈 파일이면 NotAcceptableException이 발생한다")
        void throwWhenFileEmpty() {
            User uploader = mockUser();
            MultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

            assertThatThrownBy(() -> profileImageService.uploadFile(uploader, file))
                    .isInstanceOf(NotAcceptableException.class);

            verifyNoInteractions(s3storageService, diaryImageRepository, activityLogService, snapshotFunc);
        }

        @Test
        @DisplayName("10MB 초과 파일이면 NotAcceptableException이 발생한다")
        void throwWhenFileTooLarge() {
            User uploader = mockUser();
            MultipartFile file = mock(MultipartFile.class);

            when(file.isEmpty()).thenReturn(false);
            when(file.getSize()).thenReturn(10 * 1024 * 1024 + 1L);

            assertThatThrownBy(() -> profileImageService.uploadFile(uploader, file))
                    .isInstanceOf(NotAcceptableException.class);

            verifyNoInteractions(s3storageService, diaryImageRepository, activityLogService, snapshotFunc);
        }

        @Test
        @DisplayName("mime type이 null이면 ConflictException이 발생한다")
        void throwWhenMimeTypeNull() {
            User uploader = mockUser();
            MultipartFile file = mock(MultipartFile.class);

            when(file.isEmpty()).thenReturn(false);
            when(file.getSize()).thenReturn(100L);
            when(file.getContentType()).thenReturn(null);

            assertThatThrownBy(() -> profileImageService.uploadFile(uploader, file))
                    .isInstanceOf(ConflictException.class);

            verifyNoInteractions(s3storageService, diaryImageRepository, activityLogService, snapshotFunc);
        }

        @Test
        @DisplayName("허용되지 않은 mime type이면 ConflictException이 발생한다")
        void throwWhenMimeTypeNotAllowed() {
            User uploader = mockUser();
            MultipartFile file = mock(MultipartFile.class);

            when(file.isEmpty()).thenReturn(false);
            when(file.getSize()).thenReturn(100L);
            when(file.getContentType()).thenReturn("application/x-msdownload");

            assertThatThrownBy(() -> profileImageService.uploadFile(uploader, file))
                    .isInstanceOf(ConflictException.class);

            verifyNoInteractions(s3storageService, diaryImageRepository, activityLogService, snapshotFunc);
        }

        @Test
        @DisplayName("application/octet-stream 이고 exe 파일이면 NotAcceptableException이 발생한다")
        void throwWhenExecutableFile() {
            User uploader = mockUser();
            MultipartFile file = mock(MultipartFile.class);

            when(file.isEmpty()).thenReturn(false);
            when(file.getSize()).thenReturn(100L);
            when(file.getContentType()).thenReturn("application/octet-stream");

            assertThatThrownBy(() -> profileImageService.uploadFile(uploader, file))
                    .isInstanceOf(ConflictException.class);

            verifyNoInteractions(s3storageService, diaryImageRepository, activityLogService, snapshotFunc);
        }
    }

    @Nested
    @DisplayName("uploadManyFiles()")
    class UploadManyFilesTest {

        @Test
        @DisplayName("유저가 없으면 NotFoundException이 발생한다")
        void throwWhenUserNotFound() {
            Long userId = 1L;

            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> profileImageService.uploadManyFiles(userId, List.of()))
                    .isInstanceOf(NotFoundException.class);

            verify(userRepository).findById(userId);
        }

        @Test
        @DisplayName("files가 null이면 아무 작업도 하지 않는다")
        void returnWhenFilesNull() {
            Long userId = 1L;
            User uploader = mock(User.class);

            when(userRepository.findById(userId)).thenReturn(Optional.of(uploader));
            when(uploader.getProfileImages()).thenReturn(List.of());

            profileImageService.uploadManyFiles(userId, null);

            verify(userRepository).findById(userId);
            verify(uploader).getProfileImages();
            verifyNoInteractions(s3storageService, diaryImageRepository, activityLogService, snapshotFunc);
        }

        @Test
        @DisplayName("files가 비어 있으면 아무 작업도 하지 않는다")
        void returnWhenFilesEmpty() {
            Long userId = 1L;
            User uploader = mock(User.class);

            when(userRepository.findById(userId)).thenReturn(Optional.of(uploader));
            when(uploader.getProfileImages()).thenReturn(List.of());

            profileImageService.uploadManyFiles(userId, List.of());

            verify(userRepository).findById(userId);
            verify(uploader).getProfileImages();
            verifyNoInteractions(s3storageService, diaryImageRepository, activityLogService, snapshotFunc);
        }

        @Test
        @DisplayName("요청에 없는 기존 이미지들은 삭제 대상이 된다")
        void deleteImagesNotIncludedInRequest() {
            Long userId = 1L;
            User uploader = mock(User.class);

            DiaryImage keepImage = mock(DiaryImage.class);
            DiaryImage deleteImage = mock(DiaryImage.class);

            when(keepImage.getDeleted()).thenReturn(false);
            when(keepImage.getFileName()).thenReturn("keep.png");
            when(keepImage.getSizeBytes()).thenReturn(100L);

            when(deleteImage.getDeleted()).thenReturn(false);
            when(deleteImage.getFileName()).thenReturn("delete.png");

            MultipartFile keepFile = new MockMultipartFile(
                    "file", "keep.png", "image/png", new byte[100]
            );

            when(userRepository.findById(userId)).thenReturn(Optional.of(uploader));
            when(uploader.getProfileImages()).thenReturn(List.of(keepImage, deleteImage));

            ProfileImageService spyService = spy(profileImageService);
            doNothing().when(spyService).deleteFile(any(User.class), any(DiaryImage.class));

            spyService.uploadManyFiles(userId, List.of(keepFile));

            verify(spyService).deleteFile(uploader, deleteImage);
            verify(spyService, never()).deleteFile(uploader, keepImage);
            verify(spyService, never()).uploadFile(any(User.class), any(MultipartFile.class));
        }

        @Test
        @DisplayName("DB에 없는 새 파일만 업로드한다")
        void uploadOnlyNewFiles() {
            Long userId = 1L;

            User uploader = mock(User.class);
            DiaryImage existingImage = mock(DiaryImage.class);

            when(existingImage.getFileName()).thenReturn("keep.png");
            when(existingImage.getSizeBytes()).thenReturn(100L);
            when(existingImage.getDeleted()).thenReturn(false);

            MultipartFile existingFile = file("keep.png", "image/png", 100);
            MultipartFile newFile = file("new.png", "image/png", 150);

            when(userRepository.findById(userId)).thenReturn(Optional.of(uploader));
            when(uploader.getProfileImages()).thenReturn(List.of(existingImage));

            ProfileImageService spyService = spy(profileImageService);
            doNothing().when(spyService).uploadFile(eq(uploader), eq(newFile));

            spyService.uploadManyFiles(userId, List.of(existingFile, newFile));

            verify(spyService, never()).uploadFile(eq(uploader), eq(existingFile));
            verify(spyService).uploadFile(eq(uploader), eq(newFile));
        }

        @Test
        @DisplayName("삭제된 이미지는 currentImages에서 제외된다")
        void excludesDeletedImagesFromCurrentImages() {
            Long userId = 1L;
            User uploader = mock(User.class);

            DiaryImage deletedImage = mockDiaryImage();
            MultipartFile newFile = file("new.png", "image/png", 150);

            when(userRepository.findById(userId)).thenReturn(Optional.of(uploader));
            when(uploader.getProfileImages()).thenReturn(List.of(deletedImage));

            ProfileImageService spyService = spy(profileImageService);
            doNothing().when(spyService).uploadFile(eq(uploader), eq(newFile));

            spyService.uploadManyFiles(userId, List.of(newFile));

            verify(spyService).uploadFile(eq(uploader), eq(newFile));
            verify(deletedImage).setDeleted(true);
        }
    }

    @Nested
    @DisplayName("deleteFile()")
    class DeleteFileTest {

        @Test
        @DisplayName("삭제되지 않은 이미지면 deleted=true로 바꾸고 로그를 남긴다")
        void deleteSuccess() {
            User user = mockUser();
            DiaryImage image = mockDiaryImage();

            when(image.getDeleted()).thenReturn(false);
            when(image.getId()).thenReturn(10L);
            when(image.logMessage()).thenReturn("log-message");
            when(snapshotFunc.snapshot(image)).thenReturn(new HashMap<>());

            profileImageService.deleteFile(user, image);

            verify(image).setDeleted(true);
            verify(activityLogService).log(
                    eq(ActivityEntityType.DIARY_IMAGE),
                    eq(ActivityAction.DELETE),
                    eq(10L),
                    eq("log-message"),
                    eq(user),
                    eq(new HashMap<>()),
                    isNull()
            );
        }

        @Test
        @DisplayName("이미 삭제된 이미지면 setDeleted는 호출하지 않고 로그만 남긴다")
        void logOnlyWhenAlreadyDeleted() {
            User user = mock(User.class);
            DiaryImage image = mock(DiaryImage.class);

            when(image.getDeleted()).thenReturn(true);
            when(image.getId()).thenReturn(10L);
            when(image.logMessage()).thenReturn("log-message");
            when(snapshotFunc.snapshot(image)).thenReturn(new HashMap<>());

            profileImageService.deleteFile(user, image);

            verify(image, never()).setDeleted(true);
            verify(snapshotFunc).snapshot(image);
            verify(activityLogService).log(
                    eq(ActivityEntityType.DIARY_IMAGE),
                    eq(ActivityAction.DELETE),
                    eq(10L),
                    eq("log-message"),
                    eq(user),
                    eq(new HashMap<>()),
                    isNull()
            );
        }
    }
}