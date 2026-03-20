package com.shop.sehodiary_api.service.activelog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.shop.sehodiary_api.repository.activity.ActivityAction;
import com.shop.sehodiary_api.repository.activity.ActivityEntityType;
import com.shop.sehodiary_api.repository.activity.ActivityLog;
import com.shop.sehodiary_api.repository.activity.ActivityLogRepository;
import com.shop.sehodiary_api.repository.user.User;
import com.shop.sehodiary_api.service.exceptions.NotAcceptableException;
import com.shop.sehodiary_api.web.dto.activitylog.ActivityLogResponse;
import com.shop.sehodiary_api.web.mapper.activitylog.ActivityLogMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActivityLogServiceTest {

    @Mock
    private ActivityLogRepository activityLogRepository;

    @Mock
    private ActivityLogMapper activityLogMapper;

    @InjectMocks
    private ActivityLogService activityLogService;

    @Nested
    @DisplayName("log() 테스트")
    class LogTest {

        @Test
        @DisplayName("변경사항이 있으면 ActivityLog를 저장한다")
        void log_success() {
            // given
            ActivityEntityType type = ActivityEntityType.DIARY;
            ActivityAction action = ActivityAction.UPDATE;
            Long entityId = 1L;
            String message = "제목";
            User actor = new User();
            Object beforeJson = "before";
            Object afterJson = "after";

            // when
            activityLogService.log(type, action, entityId, message, actor, beforeJson, afterJson);

            // then
            ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
            verify(activityLogRepository, times(1)).save(captor.capture());

            ActivityLog savedLog = captor.getValue();
            assertThat(savedLog.getEntityType()).isEqualTo(type);
            assertThat(savedLog.getAction()).isEqualTo(action);
            assertThat(savedLog.getEntityId()).isEqualTo(entityId);
            assertThat(savedLog.getActor()).isEqualTo(actor);
            assertThat(savedLog.getBeforeJson()).isEqualTo(beforeJson);
            assertThat(savedLog.getAfterJson()).isEqualTo(afterJson);
            assertThat(savedLog.getMessage())
                    .isEqualTo(type.toString() + "에서 " + message + " 이(가) " + action.toString() + " 되었습니다.");
        }

        @Test
        @DisplayName("변경사항이 없으면 NotAcceptableException이 발생한다")
        void log_fail_when_before_and_after_are_same() {
            // given
            ActivityEntityType type = ActivityEntityType.DIARY;
            ActivityAction action = ActivityAction.UPDATE;
            Long entityId = 1L;
            String message = "제목";
            User actor = new User();
            Object sameJson = "same";

            // when & then
            assertThatThrownBy(() ->
                    activityLogService.log(type, action, entityId, message, actor, sameJson, sameJson)
            )
                    .isInstanceOf(NotAcceptableException.class)
                    .extracting("detailMessage")
                    .isEqualTo("변경된 사항이 없습니다.");

            verify(activityLogRepository, times(0)).save(any(ActivityLog.class));
        }
    }

    @Nested
    @DisplayName("getActivityLogsByUser() 테스트")
    class GetActivityLogsByUserTest {

        @Test
        @DisplayName("유저의 활동 로그를 조회하고 response로 변환해서 반환한다")
        void getActivityLogsByUser_success() {
            // given
            Long userId = 1L;

            ActivityLog log1 = org.mockito.Mockito.mock(ActivityLog.class);
            ActivityLog log2 = org.mockito.Mockito.mock(ActivityLog.class);

            ActivityLogResponse response1 = org.mockito.Mockito.mock(ActivityLogResponse.class);
            ActivityLogResponse response2 = org.mockito.Mockito.mock(ActivityLogResponse.class);

            when(activityLogRepository.findByActorIdAndEntityTypeNot(userId, ActivityEntityType.LIKE))
                    .thenReturn(List.of(log1, log2));

            when(activityLogMapper.toResponse(log1)).thenReturn(response1);
            when(activityLogMapper.toResponse(log2)).thenReturn(response2);

            // when
            List<ActivityLogResponse> result = activityLogService.getActivityLogsByUser(userId);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(response1, response2);

            verify(activityLogRepository, times(1))
                    .findByActorIdAndEntityTypeNot(eq(userId), eq(ActivityEntityType.LIKE));
            verify(activityLogMapper, times(1)).toResponse(log1);
            verify(activityLogMapper, times(1)).toResponse(log2);
        }

        @Test
        @DisplayName("조회된 로그가 없으면 빈 리스트를 반환한다")
        void getActivityLogsByUser_empty() {
            // given
            Long userId = 1L;

            when(activityLogRepository.findByActorIdAndEntityTypeNot(userId, ActivityEntityType.LIKE))
                    .thenReturn(List.of());

            // when
            List<ActivityLogResponse> result = activityLogService.getActivityLogsByUser(userId);

            // then
            assertThat(result).isEmpty();
            verify(activityLogRepository, times(1))
                    .findByActorIdAndEntityTypeNot(userId, ActivityEntityType.LIKE);
        }
    }
}
