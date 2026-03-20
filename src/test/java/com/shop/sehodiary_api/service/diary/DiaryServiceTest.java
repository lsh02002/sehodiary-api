package com.shop.sehodiary_api.service.diary;

import com.shop.sehodiary_api.repository.activity.ActivityAction;
import com.shop.sehodiary_api.repository.activity.ActivityEntityType;
import com.shop.sehodiary_api.repository.activity.function.SnapshotFunc;
import com.shop.sehodiary_api.repository.common.Visibility;
import com.shop.sehodiary_api.repository.diary.Diary;
import com.shop.sehodiary_api.repository.diary.DiaryCacheRepository;
import com.shop.sehodiary_api.repository.diary.DiaryIdRedisRepository;
import com.shop.sehodiary_api.repository.diary.DiaryRepository;
import com.shop.sehodiary_api.repository.user.User;
import com.shop.sehodiary_api.repository.user.UserRepository;
import com.shop.sehodiary_api.service.activelog.ActivityLogService;
import com.shop.sehodiary_api.service.diaryemotion.DiaryEmotionService;
import com.shop.sehodiary_api.service.diaryimage.DiaryImageService;
import com.shop.sehodiary_api.service.exceptions.ConflictException;
import com.shop.sehodiary_api.service.exceptions.NotAcceptableException;
import com.shop.sehodiary_api.service.exceptions.NotFoundException;
import com.shop.sehodiary_api.web.dto.diary.DiaryRequest;
import com.shop.sehodiary_api.web.dto.diary.DiaryResponse;
import com.shop.sehodiary_api.web.mapper.diary.DiaryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiaryServiceTest {
    @InjectMocks
    private DiaryService diaryService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DiaryRepository diaryRepository;

    @Mock
    private DiaryImageService diaryImageService;

    @Mock
    private DiaryEmotionService diaryEmotionService;

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private DiaryMapper diaryMapper;

    @Mock
    private DiaryCacheRepository diaryCacheRepository;

    @Mock
    private DiaryIdRedisRepository diaryIdRedisRepository;

    @Mock
    private SnapshotFunc snapshotFunc;

    private User user;
    private Diary diary;
    private DiaryRequest request;
    private DiaryResponse response;
    private List<MultipartFile> files;

    @Nested
    class CreateDiaryTest {

        @BeforeEach
        void setUp() {
            user = User.builder()
                    .id(1L)
                    .build();

            request = new DiaryRequest();
            request.setTitle("오늘 일기");
            request.setContent("정말 즐거운 하루였다.");
            request.setVisibility("PUBLIC");
            request.setWeather("SUNNY");
            request.setEmoji("😊");

            response = DiaryResponse.builder()
                    .id(100L)
                    .title("오늘 일기")
                    .content("정말 즐거운 하루였다.")
                    .build();

            files = List.of(
                    new MockMultipartFile(
                            "file",
                            "test.jpg",
                            "image/jpeg",
                            "dummy image".getBytes()
                    )
            );
        }

        @Test
        @DisplayName("일기 생성 성공 - PUBLIC 공개범위")
        void createDiary_success_public() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(snapshotFunc.snapshot(any(Diary.class))).willReturn(new HashMap<>());
            given(diaryMapper.toResponse(any(Diary.class))).willReturn(response);

            willAnswer(invocation -> {
                Diary diary = invocation.getArgument(0);
                // save 이후 id가 세팅된 상황 흉내
                org.springframework.test.util.ReflectionTestUtils.setField(diary, "id", 100L);
                return diary;
            }).given(diaryRepository).save(any(Diary.class));

            // when
            DiaryResponse result = diaryService.createDiary(1L, request, files);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getTitle()).isEqualTo("오늘 일기");

            verify(userRepository).findById(1L);
            verify(diaryRepository).save(any(Diary.class));
            verify(diaryImageService).uploadManyFiles(1L, 100L, files);
            verify(diaryEmotionService).createDiaryEmotion(1L, 100L, "😊");
            verify(activityLogService).log(
                    any(), any(), eq(100L), anyString(), eq(user), isNull(), any()
            );
            verify(diaryCacheRepository).put(response);
            verify(diaryIdRedisRepository).addPublic(100L);
            verify(diaryIdRedisRepository).addUser(1L, 100L);

            verify(diaryIdRedisRepository, never()).addFriends(anyLong());
        }

        @Test
        @DisplayName("일기 생성 성공 - FRIENDS 공개범위")
        void createDiary_success_friends() {
            // given
            request.setVisibility("FRIENDS");

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(snapshotFunc.snapshot(any(Diary.class))).willReturn(new HashMap<>());
            given(diaryMapper.toResponse(any(Diary.class))).willReturn(response);

            willAnswer(invocation -> {
                Diary diary = invocation.getArgument(0);
                org.springframework.test.util.ReflectionTestUtils.setField(diary, "id", 200L);
                return diary;
            }).given(diaryRepository).save(any(Diary.class));

            // when
            diaryService.createDiary(1L, request, files);

            // then
            verify(diaryIdRedisRepository).addFriends(200L);
            verify(diaryIdRedisRepository).addUser(1L, 200L);
            verify(diaryIdRedisRepository, never()).addPublic(anyLong());
        }

        @Test
        @DisplayName("사용자가 없으면 NotFoundException 발생")
        void createDiary_fail_userNotFound() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> diaryService.createDiary(1L, request, files))
                    .isInstanceOf(NotFoundException.class)
                    .extracting("detailMessage")
                    .isEqualTo("해당 사용자를 찾을 수 없습니다.");

            verify(diaryRepository, never()).save(any());
            verify(diaryImageService, never()).uploadManyFiles(anyLong(), anyLong(), anyList());
            verify(diaryEmotionService, never()).createDiaryEmotion(anyLong(), anyLong(), anyString());
        }

        @Nested
        @DisplayName("입력값 검증 실패")
        class ValidationFailTest {

            @Test
            @DisplayName("제목이 비어있으면 NotAcceptableException 발생")
            void createDiary_fail_emptyTitle() {
                // given
                request.setTitle("   ");
                given(userRepository.findById(1L)).willReturn(Optional.of(user));

                // when & then
                assertThatThrownBy(() -> diaryService.createDiary(1L, request, files))
                        .isInstanceOf(NotAcceptableException.class)
                        .extracting("detailMessage")
                        .isEqualTo("해당 제목란이 비어있습니다");

                verify(diaryRepository, never()).save(any());
            }

            @Test
            @DisplayName("내용이 비어있으면 NotAcceptableException 발생")
            void createDiary_fail_emptyContent() {
                // given
                request.setContent("   ");
                given(userRepository.findById(1L)).willReturn(Optional.of(user));

                // when & then
                assertThatThrownBy(() -> diaryService.createDiary(1L, request, files))
                        .isInstanceOf(NotAcceptableException.class)
                        .extracting("detailMessage")
                        .isEqualTo("해당 내용란이 비어있습니다");

                verify(diaryRepository, never()).save(any());
            }

            @Test
            @DisplayName("공개여부가 비어있으면 NotAcceptableException 발생")
            void createDiary_fail_emptyVisibility() {
                // given
                request.setVisibility("   ");
                given(userRepository.findById(1L)).willReturn(Optional.of(user));

                // when & then
                assertThatThrownBy(() -> diaryService.createDiary(1L, request, files))
                        .isInstanceOf(NotAcceptableException.class)
                        .extracting("detailMessage")
                        .isEqualTo("해당 공개여부란이 비어있습니다");

                verify(diaryRepository, never()).save(any());
            }

            @Test
            @DisplayName("날씨가 비어있으면 NotAcceptableException 발생")
            void createDiary_fail_emptyWeather() {
                // given
                request.setWeather("   ");
                given(userRepository.findById(1L)).willReturn(Optional.of(user));

                // when & then
                assertThatThrownBy(() -> diaryService.createDiary(1L, request, files))
                        .isInstanceOf(NotAcceptableException.class)
                        .extracting("detailMessage")
                        .isEqualTo("해당 날씨란이 비어있습니다");

                verify(diaryRepository, never()).save(any());
            }
        }
    }

    @Nested
    class EditDiaryTest {

        @BeforeEach
        void setUp() {
            user = User.builder()
                    .id(1L)
                    .build();

            diary = Diary.builder()
                    .user(user)
                    .title("기존 제목")
                    .content("기존 내용")
                    .visibility(Visibility.PRIVATE)
                    .weather("RAINY")
                    .build();

            org.springframework.test.util.ReflectionTestUtils.setField(diary, "id", 10L);

            request = new DiaryRequest();
            request.setTitle("수정된 제목");
            request.setContent("수정된 내용");
            request.setVisibility("PUBLIC");
            request.setWeather("SUNNY");
            request.setEmoji("😊");

            response = DiaryResponse.builder()
                    .id(10L)
                    .title("수정된 제목")
                    .content("수정된 내용")
                    .build();

            files = List.of(
                    new MockMultipartFile(
                            "file",
                            "image.jpg",
                            "image/jpeg",
                            "dummy".getBytes()
                    )
            );
        }

        @Test
        @DisplayName("일기 수정 성공 - 모든 값 변경 + PUBLIC 반영")
        void editDiary_success_allChanged_public() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(diaryRepository.findByUserIdAndId(1L, 10L))
                    .willReturn(Optional.of(diary))
                    .willReturn(Optional.of(diary)); // flush 후 재조회
            given(snapshotFunc.snapshot(any(Diary.class)))
                    .willReturn(new HashMap<>())
                    .willReturn(new HashMap<>());
            given(diaryMapper.toResponse(any(Diary.class))).willReturn(response);

            // when
            DiaryResponse result = diaryService.editDiary(1L, 10L, request, files);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(10L);
            assertThat(diary.getTitle()).isEqualTo("수정된 제목");
            assertThat(diary.getContent()).isEqualTo("수정된 내용");
            assertThat(diary.getVisibility()).isEqualTo(Visibility.PUBLIC);
            assertThat(diary.getWeather()).isEqualTo("SUNNY");

            verify(userRepository).findById(1L);
            verify(diaryRepository, times(2)).findByUserIdAndId(1L, 10L);
            verify(diaryImageService).uploadManyFiles(1L, 10L, files);
            verify(diaryEmotionService).editDiaryEmotion(1L, 10L, "😊");
            verify(diaryRepository).flush();
            verify(activityLogService).log(
                    any(),
                    any(),
                    eq(10L),
                    anyString(),
                    eq(user),
                    eq(new HashMap<>()),
                    eq(new HashMap<>())
            );
            verify(diaryCacheRepository).put(response);
            verify(diaryIdRedisRepository).addPublic(10L);
            verify(diaryIdRedisRepository, never()).addFriends(anyLong());
        }

        @Test
        @DisplayName("일기 수정 성공 - FRIENDS 반영")
        void editDiary_success_friends() {
            // given
            request.setVisibility("FRIENDS");

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(diaryRepository.findByUserIdAndId(1L, 10L))
                    .willReturn(Optional.of(diary))
                    .willReturn(Optional.of(diary));
            given(snapshotFunc.snapshot(any(Diary.class)))
                    .willReturn(new HashMap<>())
                    .willReturn(new HashMap<>());
            given(diaryMapper.toResponse(any(Diary.class))).willReturn(response);

            // when
            diaryService.editDiary(1L, 10L, request, files);

            // then
            verify(diaryIdRedisRepository).addFriends(10L);
            verify(diaryIdRedisRepository, never()).addPublic(anyLong());
        }

        @Test
        @DisplayName("emoji가 null이면 감정 수정 호출 안 함")
        void editDiary_success_noEmoji() {
            // given
            request.setEmoji(null);

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(diaryRepository.findByUserIdAndId(1L, 10L))
                    .willReturn(Optional.of(diary))
                    .willReturn(Optional.of(diary));
            given(snapshotFunc.snapshot(any(Diary.class)))
                    .willReturn(new HashMap<>())
                    .willReturn(new HashMap<>());
            given(diaryMapper.toResponse(any(Diary.class))).willReturn(response);

            // when
            diaryService.editDiary(1L, 10L, request, files);

            // then
            verify(diaryEmotionService, never()).editDiaryEmotion(anyLong(), anyLong(), anyString());
            verify(diaryImageService).uploadManyFiles(1L, 10L, files);
            verify(diaryRepository).flush();
        }

        @Test
        @DisplayName("emoji가 공백이면 감정 수정 호출 안 함")
        void editDiary_success_blankEmoji() {
            // given
            request.setEmoji("   ");

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(diaryRepository.findByUserIdAndId(1L, 10L))
                    .willReturn(Optional.of(diary))
                    .willReturn(Optional.of(diary));
            given(snapshotFunc.snapshot(any(Diary.class)))
                    .willReturn(new HashMap<>())
                    .willReturn(new HashMap<>());
            given(diaryMapper.toResponse(any(Diary.class))).willReturn(response);

            // when
            diaryService.editDiary(1L, 10L, request, files);

            // then
            verify(diaryEmotionService, never()).editDiaryEmotion(anyLong(), anyLong(), anyString());
        }

        @Test
        @DisplayName("값이 동일해도 정상 동작하고 flush 및 부가 로직 수행")
        void editDiary_success_sameValues() {
            // given
            request.setTitle("기존 제목");
            request.setContent("기존 내용");
            request.setVisibility("PRIVATE");
            request.setWeather("RAINY");
            request.setEmoji("🙂");

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(diaryRepository.findByUserIdAndId(1L, 10L))
                    .willReturn(Optional.of(diary))
                    .willReturn(Optional.of(diary));
            given(snapshotFunc.snapshot(any(Diary.class)))
                    .willReturn(new HashMap<>())
                    .willReturn(new HashMap<>());
            given(diaryMapper.toResponse(any(Diary.class))).willReturn(response);

            // when
            DiaryResponse result = diaryService.editDiary(1L, 10L, request, files);

            // then
            assertThat(result).isNotNull();
            assertThat(diary.getTitle()).isEqualTo("기존 제목");
            assertThat(diary.getContent()).isEqualTo("기존 내용");
            assertThat(diary.getVisibility()).isEqualTo(Visibility.PRIVATE);
            assertThat(diary.getWeather()).isEqualTo("RAINY");

            verify(diaryImageService).uploadManyFiles(1L, 10L, files);
            verify(diaryEmotionService).editDiaryEmotion(1L, 10L, "🙂");
            verify(diaryRepository).flush();
            verify(diaryCacheRepository).put(response);

            verify(diaryIdRedisRepository, never()).addPublic(anyLong());
            verify(diaryIdRedisRepository, never()).addFriends(anyLong());
        }

        @Nested
        @DisplayName("예외 케이스")
        class ExceptionTest {

            @Test
            @DisplayName("사용자가 없으면 NotFoundException")
            void editDiary_fail_userNotFound() {
                // given
                given(userRepository.findById(1L)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> diaryService.editDiary(1L, 10L, request, files))
                        .isInstanceOf(NotFoundException.class)
                        .extracting("detailMessage")
                        .isEqualTo("해당 사용자를 찾을 수 없습니다.");

                verify(diaryRepository, never()).findByUserIdAndId(anyLong(), anyLong());
                verify(diaryImageService, never()).uploadManyFiles(anyLong(), anyLong(), anyList());
                verify(diaryRepository, never()).flush();
            }

            @Test
            @DisplayName("해당 유저의 일기가 아니면 NotFoundException")
            void editDiary_fail_diaryNotFound() {
                // given
                given(userRepository.findById(1L)).willReturn(Optional.of(user));
                given(diaryRepository.findByUserIdAndId(1L, 10L)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> diaryService.editDiary(1L, 10L, request, files))
                        .isInstanceOf(NotFoundException.class)
                        .extracting("detailMessage")
                        .isEqualTo("해당 사용자가 작성한 글이 아닙니다");

                verify(diaryImageService, never()).uploadManyFiles(anyLong(), anyLong(), anyList());
                verify(diaryEmotionService, never()).editDiaryEmotion(anyLong(), anyLong(), anyString());
                verify(diaryRepository, never()).flush();
            }

            @Test
            @DisplayName("flush 후 재조회에서 없으면 NotFoundException")
            void editDiary_fail_reloadedDiaryNotFound() {
                // given
                given(userRepository.findById(1L)).willReturn(Optional.of(user));
                given(diaryRepository.findByUserIdAndId(1L, 10L))
                        .willReturn(Optional.of(diary))
                        .willReturn(Optional.empty());
                given(snapshotFunc.snapshot(any(Diary.class))).willReturn(new HashMap<>());

                // when & then
                assertThatThrownBy(() -> diaryService.editDiary(1L, 10L, request, files))
                        .isInstanceOf(NotFoundException.class)
                        .extracting("detailMessage")
                        .isEqualTo("해당 사용자가 작성한 글이 아닙니다");

                verify(diaryImageService).uploadManyFiles(1L, 10L, files);
                verify(diaryRepository).flush();
                verify(activityLogService, never()).log(any(), any(), anyLong(), anyString(), any(), any(), any());
            }
        }
    }

    @Nested
    class DeleteDiaryTest {
        @Test
        @DisplayName("일기 삭제 성공")
        void deleteDiary_success() {
            // given
            Long userId = 1L;
            Long diaryId = 10L;

            User user = User.builder()
                    .id(userId)
                    .build();

            Diary diary = Diary.builder()
                    .user(user)
                    .title("삭제할 일기")
                    .content("삭제할 내용")
                    .visibility(Visibility.PUBLIC)
                    .weather("SUNNY")
                    .build();

            ReflectionTestUtils.setField(diary, "id", diaryId);

            Object beforeSnapshot = new HashMap<>();

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(diaryRepository.findByUserIdAndId(userId, diaryId)).willReturn(Optional.of(diary));
            given(snapshotFunc.snapshot(diary)).willReturn(new HashMap<>());

            // when
            diaryService.deleteDiary(userId, diaryId);

            // then
            verify(userRepository).findById(userId);
            verify(diaryRepository).findByUserIdAndId(userId, diaryId);
            verify(diaryImageService).deleteManyFiles(diary);
            verify(snapshotFunc).snapshot(diary);
            verify(activityLogService).log(
                    eq(ActivityEntityType.DIARY),
                    eq(ActivityAction.DELETE),
                    eq(diaryId),
                    anyString(),
                    eq(user),
                    eq(beforeSnapshot),
                    isNull()
            );
            verify(diaryRepository).deleteByUserIdAndId(userId, diaryId);
            verify(diaryCacheRepository).delete(diaryId);
            verify(diaryIdRedisRepository).remove(diaryId);
            verify(diaryIdRedisRepository).removeFromUser(userId, diaryId);
        }

        @Nested
        @DisplayName("예외 케이스")
        class ExceptionTest {

            @Test
            @DisplayName("사용자가 없으면 ConflictException 발생")
            void deleteDiary_fail_userNotFound() {
                // given
                Long userId = 1L;
                Long diaryId = 10L;

                given(userRepository.findById(userId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> diaryService.deleteDiary(userId, diaryId))
                        .isInstanceOf(ConflictException.class)
                        .extracting("detailMessage")
                        .isEqualTo("해당 글을 삭제할 수 없습니다");

                verify(userRepository).findById(userId);
                verify(diaryRepository, never()).findByUserIdAndId(anyLong(), anyLong());
                verify(diaryImageService, never()).deleteManyFiles(any());
                verify(diaryRepository, never()).deleteByUserIdAndId(anyLong(), anyLong());
                verify(diaryCacheRepository, never()).delete(anyLong());
                verify(diaryIdRedisRepository, never()).remove(anyLong());
                verify(diaryIdRedisRepository, never()).removeFromUser(anyLong(), anyLong());
            }

            @Test
            @DisplayName("해당 유저의 일기가 아니면 ConflictException 발생")
            void deleteDiary_fail_diaryNotFound() {
                // given
                Long userId = 1L;
                Long diaryId = 10L;

                User user = User.builder()
                        .id(userId)
                        .build();

                given(userRepository.findById(userId)).willReturn(Optional.of(user));
                given(diaryRepository.findByUserIdAndId(userId, diaryId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> diaryService.deleteDiary(userId, diaryId))
                        .isInstanceOf(ConflictException.class)
                        .extracting("detailMessage")
                        .isEqualTo("해당 글을 삭제할 수 없습니다");

                verify(diaryImageService, never()).deleteManyFiles(any());
                verify(diaryRepository, never()).deleteByUserIdAndId(anyLong(), anyLong());
                verify(diaryCacheRepository, never()).delete(anyLong());
                verify(diaryIdRedisRepository, never()).remove(anyLong());
                verify(diaryIdRedisRepository, never()).removeFromUser(anyLong(), anyLong());
            }

            @Test
            @DisplayName("이미지 삭제 중 예외 발생 시 ConflictException 발생")
            void deleteDiary_fail_deleteFiles() {
                // given
                Long userId = 1L;
                Long diaryId = 10L;

                User user = User.builder()
                        .id(userId)
                        .build();

                Diary diary = Diary.builder()
                        .user(user)
                        .title("삭제할 일기")
                        .content("삭제할 내용")
                        .visibility(Visibility.PUBLIC)
                        .weather("SUNNY")
                        .build();

                ReflectionTestUtils.setField(diary, "id", diaryId);

                given(userRepository.findById(userId)).willReturn(Optional.of(user));
                given(diaryRepository.findByUserIdAndId(userId, diaryId)).willReturn(Optional.of(diary));
                doThrow(new RuntimeException("S3 삭제 실패"))
                        .when(diaryImageService).deleteManyFiles(diary);

                // when & then
                assertThatThrownBy(() -> diaryService.deleteDiary(userId, diaryId))
                        .isInstanceOf(ConflictException.class)
                        .extracting("detailMessage")
                        .isEqualTo("해당 글을 삭제할 수 없습니다");

                verify(diaryImageService).deleteManyFiles(diary);
                verify(activityLogService, never()).log(any(), any(), anyLong(), anyString(), any(), any(), any());
                verify(diaryRepository, never()).deleteByUserIdAndId(anyLong(), anyLong());
                verify(diaryCacheRepository, never()).delete(anyLong());
                verify(diaryIdRedisRepository, never()).remove(anyLong());
                verify(diaryIdRedisRepository, never()).removeFromUser(anyLong(), anyLong());
            }

            @Test
            @DisplayName("DB 삭제 중 예외 발생 시 ConflictException 발생")
            void deleteDiary_fail_deleteRepository() {
                // given
                Long userId = 1L;
                Long diaryId = 10L;

                User user = User.builder()
                        .id(userId)
                        .build();

                Diary diary = Diary.builder()
                        .user(user)
                        .title("삭제할 일기")
                        .content("삭제할 내용")
                        .visibility(Visibility.PUBLIC)
                        .weather("SUNNY")
                        .build();

                ReflectionTestUtils.setField(diary, "id", diaryId);

                Object beforeSnapshot = new HashMap<>();

                given(userRepository.findById(userId)).willReturn(Optional.of(user));
                given(diaryRepository.findByUserIdAndId(userId, diaryId)).willReturn(Optional.of(diary));
                given(snapshotFunc.snapshot(diary)).willReturn(new HashMap<>());

                doThrow(new RuntimeException("DB 삭제 실패"))
                        .when(diaryRepository).deleteByUserIdAndId(userId, diaryId);

                // when & then
                assertThatThrownBy(() -> diaryService.deleteDiary(userId, diaryId))
                        .isInstanceOf(ConflictException.class)
                        .extracting("detailMessage")
                        .isEqualTo("해당 글을 삭제할 수 없습니다");

                verify(diaryImageService).deleteManyFiles(diary);
                verify(activityLogService).log(
                        eq(ActivityEntityType.DIARY),
                        eq(ActivityAction.DELETE),
                        eq(diaryId),
                        anyString(),
                        eq(user),
                        eq(beforeSnapshot),
                        isNull()
                );
                verify(diaryRepository).deleteByUserIdAndId(userId, diaryId);
                verify(diaryCacheRepository, never()).delete(anyLong());
                verify(diaryIdRedisRepository, never()).remove(anyLong());
                verify(diaryIdRedisRepository, never()).removeFromUser(anyLong(), anyLong());
            }
        }
    }
}
