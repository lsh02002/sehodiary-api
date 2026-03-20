package com.shop.sehodiary_api.service.emotion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.shop.sehodiary_api.repository.activity.ActivityAction;
import com.shop.sehodiary_api.repository.activity.ActivityEntityType;
import com.shop.sehodiary_api.repository.activity.function.SnapshotFunc;
import com.shop.sehodiary_api.repository.emotion.Emotion;
import com.shop.sehodiary_api.repository.emotion.EmotionRepository;
import com.shop.sehodiary_api.repository.user.User;
import com.shop.sehodiary_api.repository.user.UserRepository;
import com.shop.sehodiary_api.repository.user.userRoles.Roles;
import com.shop.sehodiary_api.repository.user.userRoles.UserRoles;
import com.shop.sehodiary_api.service.activelog.ActivityLogService;
import com.shop.sehodiary_api.service.exceptions.BadRequestException;
import com.shop.sehodiary_api.service.exceptions.ConflictException;
import com.shop.sehodiary_api.service.exceptions.NotAcceptableException;
import com.shop.sehodiary_api.service.exceptions.NotFoundException;
import com.shop.sehodiary_api.web.dto.emotion.EmotionRequest;
import com.shop.sehodiary_api.web.dto.emotion.EmotionResponse;
import com.shop.sehodiary_api.web.mapper.emotion.EmotionMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmotionServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private EmotionRepository emotionRepository;
    @Mock
    private ActivityLogService activityLogService;
    @Mock
    private SnapshotFunc snapshotFunc;
    @Mock
    private EmotionMapper emotionMapper;

    @InjectMocks
    private EmotionService emotionService;

    User adminUser = adminUser();
    User nonAdminUser = nonAdminUser();

    @Nested
    @DisplayName("createEmotion()")
    class CreateEmotionTest {

        @Test
        @DisplayName("성공: 관리자가 새 이모션을 생성한다")
        void createEmotion_success() {
            Long userId = 1L;
            Long emotionId = 100L;

            EmotionRequest request = new EmotionRequest("happy", "😊");
            User adminUser = adminUser();
            EmotionResponse response = mock(EmotionResponse.class);

            given(userRepository.findById(userId)).willReturn(Optional.of(adminUser));
            given(emotionRepository.existsByName("happy")).willReturn(false);
            given(emotionRepository.existsByEmoji("😊")).willReturn(false);

            given(emotionRepository.save(any(Emotion.class))).willAnswer(invocation -> {
                Emotion emotion = invocation.getArgument(0);
                ReflectionTestUtils.setField(emotion, "id", emotionId);
                return emotion;
            });

            given(snapshotFunc.snapshot(any(Emotion.class)))
                    .willReturn(Map.of("id", emotionId, "name", "happy", "emoji", "😊"));

            given(emotionMapper.toResponse(any(Emotion.class))).willReturn(response);

            EmotionResponse result = emotionService.createEmotion(userId, request);

            assertEquals(response, result);

            verify(userRepository).findById(userId);
            verify(emotionRepository).existsByName("happy");
            verify(emotionRepository).existsByEmoji("😊");
            verify(emotionRepository).save(any(Emotion.class));

            verify(activityLogService).log(
                    eq(ActivityEntityType.EMOTION),
                    eq(ActivityAction.CREATE),
                    eq(emotionId),
                    any(String.class),
                    eq(adminUser),
                    isNull(),
                    any()
            );

            verify(emotionMapper).toResponse(any(Emotion.class));
        }

        @Test
        @DisplayName("실패: 사용자가 없으면 NotFoundException")
        void createEmotion_fail_userNotFound() {
            Long userId = 1L;
            EmotionRequest request = new EmotionRequest("happy", "😊");

            given(userRepository.findById(userId)).willReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> emotionService.createEmotion(userId, request));

            verify(emotionRepository, never()).save(any());
            verify(activityLogService, never()).log(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("실패: 관리자가 아니면 BadRequestException")
        void createEmotion_fail_notAdmin() {
            Long userId = 1L;
            EmotionRequest request = new EmotionRequest("happy", "😊");

            User nonAdminUser = nonAdminUser();

            given(userRepository.findById(userId)).willReturn(Optional.of(nonAdminUser));

            assertThrows(BadRequestException.class,
                    () -> emotionService.createEmotion(userId, request));

            verify(emotionRepository, never()).existsByName(any());
            verify(emotionRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패: 이름이 공란이면 NotAcceptableException")
        void createEmotion_fail_blankName() {
            Long userId = 1L;
            EmotionRequest request = new EmotionRequest("   ", "😊");

            given(userRepository.findById(userId)).willReturn(Optional.of(adminUser));

            assertThrows(NotAcceptableException.class,
                    () -> emotionService.createEmotion(userId, request));

            verify(emotionRepository, never()).existsByName(any());
            verify(emotionRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패: 이모지가 공란이면 NotAcceptableException")
        void createEmotion_fail_blankEmoji() {
            Long userId = 1L;
            EmotionRequest request = new EmotionRequest("happy", "   ");

            given(userRepository.findById(userId)).willReturn(Optional.of(adminUser));

            assertThrows(NotAcceptableException.class,
                    () -> emotionService.createEmotion(userId, request));

            verify(emotionRepository, never()).existsByName(any());
            verify(emotionRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패: 이름이 중복이면 NotAcceptableException")
        void createEmotion_fail_duplicateName() {
            Long userId = 1L;
            EmotionRequest request = new EmotionRequest("happy", "😊");

            given(userRepository.findById(userId)).willReturn(Optional.of(adminUser));
            given(emotionRepository.existsByName("happy")).willReturn(true);

            assertThrows(NotAcceptableException.class,
                    () -> emotionService.createEmotion(userId, request));

            verify(emotionRepository, never()).existsByEmoji(any());
            verify(emotionRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패: 이모지가 중복이면 NotAcceptableException")
        void createEmotion_fail_duplicateEmoji() {
            Long userId = 1L;
            EmotionRequest request = new EmotionRequest("happy", "😊");

            given(userRepository.findById(userId)).willReturn(Optional.of(adminUser));
            given(emotionRepository.existsByName("happy")).willReturn(false);
            given(emotionRepository.existsByEmoji("😊")).willReturn(true);

            assertThrows(NotAcceptableException.class,
                    () -> emotionService.createEmotion(userId, request));

            verify(emotionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("editEmotion()")
    class EditEmotionTest {

        @Test
        @DisplayName("성공: 관리자가 이름과 이모지를 수정한다")
        void editEmotion_success() {
            Long userId = 1L;
            Long emotionId = 10L;

            EmotionRequest request = new EmotionRequest("sad", "😢");
            User adminUser = adminUser();
            Emotion emotion = existingEmotion(emotionId, "happy", "😊");
            EmotionResponse response = mock(EmotionResponse.class);

            given(userRepository.findById(userId)).willReturn(Optional.of(adminUser));
            given(emotionRepository.existsByName("sad")).willReturn(false);
            given(emotionRepository.existsByEmoji("😢")).willReturn(false);
            given(emotionRepository.findById(emotionId)).willReturn(Optional.of(emotion));

            given(snapshotFunc.snapshot(emotion))
                    .willReturn(Map.of("name", "happy", "emoji", "😊"))
                    .willReturn(Map.of("name", "sad", "emoji", "😢"));

            given(emotionMapper.toResponse(emotion)).willReturn(response);

            EmotionResponse result = emotionService.editEmotion(userId, emotionId, request);

            assertEquals(response, result);
            assertEquals("sad", emotion.getName());
            assertEquals("😢", emotion.getEmoji());

            verify(activityLogService).log(
                    eq(ActivityEntityType.EMOTION),
                    eq(ActivityAction.UPDATE),
                    eq(emotionId),
                    any(String.class),
                    eq(adminUser),
                    any(),
                    any()
            );
            verify(emotionMapper).toResponse(emotion);
        }

        @Test
        @DisplayName("성공: 값이 같아도 로그는 남긴다")
        void editEmotion_success_sameValuesStillLogs() {
            Long userId = 1L;
            Long emotionId = 10L;

            EmotionRequest request = new EmotionRequest("happy", "😊");
            User adminUser = adminUser();
            Emotion emotion = existingEmotion(emotionId, "happy", "😊");
            EmotionResponse response = mock(EmotionResponse.class);

            given(userRepository.findById(userId)).willReturn(Optional.of(adminUser));
            given(emotionRepository.existsByName("happy")).willReturn(false);
            given(emotionRepository.existsByEmoji("😊")).willReturn(false);
            given(emotionRepository.findById(emotionId)).willReturn(Optional.of(emotion));

            given(snapshotFunc.snapshot(emotion))
                    .willReturn(Map.of("name", "happy", "emoji", "😊"))
                    .willReturn(Map.of("name", "happy", "emoji", "😊"));

            given(emotionMapper.toResponse(emotion)).willReturn(response);

            EmotionResponse result = emotionService.editEmotion(userId, emotionId, request);

            assertEquals(response, result);

            verify(activityLogService).log(
                    eq(ActivityEntityType.EMOTION),
                    eq(ActivityAction.UPDATE),
                    eq(emotionId),
                    any(String.class),
                    eq(adminUser),
                    any(),
                    any()
            );
        }

        @Test
        @DisplayName("실패: 사용자가 없으면 NotFoundException")
        void editEmotion_fail_userNotFound() {
            Long userId = 1L;
            Long emotionId = 10L;
            EmotionRequest request = new EmotionRequest("sad", "😢");

            given(userRepository.findById(userId)).willReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> emotionService.editEmotion(userId, emotionId, request));

            verify(emotionRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("실패: 관리자가 아니면 BadRequestException")
        void editEmotion_fail_notAdmin() {
            Long userId = 1L;
            Long emotionId = 10L;
            EmotionRequest request = new EmotionRequest("sad", "😢");

            given(userRepository.findById(userId)).willReturn(Optional.of(nonAdminUser));

            assertThrows(BadRequestException.class,
                    () -> emotionService.editEmotion(userId, emotionId, request));

            verify(emotionRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("실패: 이름이 중복이면 NotAcceptableException")
        void editEmotion_fail_duplicateName() {
            Long userId = 1L;
            Long emotionId = 10L;
            EmotionRequest request = new EmotionRequest("sad", "😢");

            given(userRepository.findById(userId)).willReturn(Optional.of(adminUser));
            given(emotionRepository.existsByName("sad")).willReturn(true);

            assertThrows(NotAcceptableException.class,
                    () -> emotionService.editEmotion(userId, emotionId, request));

            verify(emotionRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("실패: 이모지가 중복이면 NotAcceptableException")
        void editEmotion_fail_duplicateEmoji() {
            Long userId = 1L;
            Long emotionId = 10L;
            EmotionRequest request = new EmotionRequest("sad", "😢");

            given(userRepository.findById(userId)).willReturn(Optional.of(adminUser));
            given(emotionRepository.existsByName("sad")).willReturn(false);
            given(emotionRepository.existsByEmoji("😢")).willReturn(true);

            assertThrows(NotAcceptableException.class,
                    () -> emotionService.editEmotion(userId, emotionId, request));

            verify(emotionRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("실패: 이모션이 없으면 NotFoundException")
        void editEmotion_fail_emotionNotFound() {
            Long userId = 1L;
            Long emotionId = 10L;
            EmotionRequest request = new EmotionRequest("sad", "😢");

            given(userRepository.findById(userId)).willReturn(Optional.of(adminUser));
            given(emotionRepository.existsByName("sad")).willReturn(false);
            given(emotionRepository.existsByEmoji("😢")).willReturn(false);
            given(emotionRepository.findById(emotionId)).willReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> emotionService.editEmotion(userId, emotionId, request));
        }
    }

    @Nested
    @DisplayName("deleteEmotion()")
    class DeleteEmotionTest {

        @Test
        @DisplayName("성공: 관리자가 이모션을 삭제한다")
        void deleteEmotion_success() {
            Long userId = 1L;
            Long emotionId = 10L;

            User adminUser = adminUser();
            Emotion emotion = existingEmotion(emotionId, "happy", "😊");

            given(userRepository.findById(userId)).willReturn(Optional.of(adminUser));
            given(emotionRepository.findById(emotionId)).willReturn(Optional.of(emotion));
            given(snapshotFunc.snapshot(emotion))
                    .willReturn(Map.of("id", emotionId, "name", "happy", "emoji", "😊"));
            doNothing().when(emotionRepository).delete(emotion);

            emotionService.deleteEmotion(userId, emotionId);

            verify(activityLogService).log(
                    eq(ActivityEntityType.EMOTION),
                    eq(ActivityAction.DELETE),
                    eq(emotionId),
                    any(String.class),
                    eq(adminUser),
                    any(),
                    isNull()
            );
            verify(emotionRepository).delete(emotion);
        }

        @Test
        @DisplayName("실패: 사용자가 없으면 ConflictException으로 감싼다")
        void deleteEmotion_fail_userNotFound_wrappedConflict() {
            Long userId = 1L;
            Long emotionId = 10L;

            given(userRepository.findById(userId)).willReturn(Optional.empty());

            assertThrows(ConflictException.class,
                    () -> emotionService.deleteEmotion(userId, emotionId));

            verify(emotionRepository, never()).delete(any());
        }

        @Test
        @DisplayName("실패: 관리자가 아니면 ConflictException으로 감싼다")
        void deleteEmotion_fail_notAdmin_wrappedConflict() {
            Long userId = 1L;
            Long emotionId = 10L;

            given(userRepository.findById(userId)).willReturn(Optional.of(nonAdminUser));

            assertThrows(ConflictException.class,
                    () -> emotionService.deleteEmotion(userId, emotionId));

            verify(emotionRepository, never()).findById(anyLong());
            verify(emotionRepository, never()).delete(any());
        }

        @Test
        @DisplayName("실패: 이모션이 없으면 ConflictException으로 감싼다")
        void deleteEmotion_fail_emotionNotFound_wrappedConflict() {
            Long userId = 1L;
            Long emotionId = 10L;

            given(userRepository.findById(userId)).willReturn(Optional.of(adminUser));
            given(emotionRepository.findById(emotionId)).willReturn(Optional.empty());

            assertThrows(ConflictException.class,
                    () -> emotionService.deleteEmotion(userId, emotionId));

            verify(emotionRepository, never()).delete(any());
        }

        @Test
        @DisplayName("실패: 삭제 중 예외가 나면 ConflictException")
        void deleteEmotion_fail_deleteException() {
            Long userId = 1L;
            Long emotionId = 10L;

            User adminUser = adminUser();
            Emotion emotion = existingEmotion(emotionId, "happy", "😊");

            given(userRepository.findById(userId)).willReturn(Optional.of(adminUser));
            given(emotionRepository.findById(emotionId)).willReturn(Optional.of(emotion));
            given(snapshotFunc.snapshot(emotion))
                    .willReturn(Map.of("id", emotionId, "name", "happy", "emoji", "😊"));

            org.mockito.Mockito.doThrow(new RuntimeException("db error"))
                    .when(emotionRepository).delete(emotion);

            assertThrows(ConflictException.class,
                    () -> emotionService.deleteEmotion(userId, emotionId));
        }
    }

    private User adminUser() {
        User user = mock(User.class);
        UserRoles userRoles = mock(UserRoles.class);
        Roles roles = mock(Roles.class);

        given(user.getUserRoles()).willReturn(List.of(userRoles));
        given(userRoles.getRoles()).willReturn(roles);
        given(roles.getName()).willReturn("ROLE_ADMIN");

        return user;
    }

    private User nonAdminUser() {
        User user = mock(User.class);
        UserRoles userRoles = mock(UserRoles.class);
        Roles roles = mock(Roles.class);

        given(user.getUserRoles()).willReturn(List.of(userRoles));
        given(userRoles.getRoles()).willReturn(roles);
        given(roles.getName()).willReturn("ROLE_USER");

        return user;
    }

    private Emotion existingEmotion(Long id, String name, String emoji) {
        Emotion emotion = Emotion.builder()
                .name(name)
                .emoji(emoji)
                .build();

        ReflectionTestUtils.setField(emotion, "id", id);
        return emotion;
    }
}