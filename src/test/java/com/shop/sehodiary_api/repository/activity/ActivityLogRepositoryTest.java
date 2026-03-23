package com.shop.sehodiary_api.repository.activity;

import com.shop.sehodiary_api.TestUserFactory;
import com.shop.sehodiary_api.repository.user.User;
import com.shop.sehodiary_api.repository.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@EnableJpaAuditing
class ActivityLogRepositoryTest {

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("actorId + entityType 제외 조건 조회")
    void findByActorIdAndEntityTypeNot() {
        // given
        User actor = userRepository.save(TestUserFactory.createUser());

        User otherActor = userRepository.save(TestUserFactory.createUser());

        ActivityLog diaryLog = ActivityLog.builder()
                .actor(actor)
                .entityType(ActivityEntityType.DIARY)
                .entityId(1L)
                .action(ActivityAction.CREATE)
                .build();

        ActivityLog commentLog = ActivityLog.builder()
                .actor(actor)
                .entityType(ActivityEntityType.COMMENT)
                .entityId(2L)
                .action(ActivityAction.CREATE)
                .build();

        ActivityLog likeLog = ActivityLog.builder()
                .actor(actor)
                .entityType(ActivityEntityType.LIKE)
                .entityId(3L)
                .action(ActivityAction.UPDATE)
                .build();

        ActivityLog otherUserLog = ActivityLog.builder()
                .actor(otherActor)
                .entityType(ActivityEntityType.DIARY)
                .entityId(4L)
                .action(ActivityAction.CREATE)
                .build();

        activityLogRepository.saveAll(List.of(
                diaryLog, commentLog, likeLog, otherUserLog
        ));

        // when
        List<ActivityLog> result =
                activityLogRepository.findByActorIdAndEntityTypeNot(
                        actor.getId(),
                        ActivityEntityType.COMMENT
                );

        // then
        assertThat(result).hasSize(2);

        // actor 필터 확인
        assertThat(result)
                .extracting(log -> log.getActor().getId())
                .containsOnly(actor.getId());

        // 제외 조건 확인 (COMMENT 없어야 함)
        assertThat(result)
                .extracting(ActivityLog::getEntityType)
                .containsExactlyInAnyOrder(
                        ActivityEntityType.DIARY,
                        ActivityEntityType.LIKE
                );
    }

    @Test
    @DisplayName("제외 조건 때문에 결과가 없는 경우")
    void returnsEmptyWhenAllFilteredOut() {
        // given
        User actor = userRepository.save(TestUserFactory.createUser());

        ActivityLog commentLog = ActivityLog.builder()
                .actor(actor)
                .entityType(ActivityEntityType.COMMENT)
                .entityId(1L)
                .action(ActivityAction.CREATE)
                .build();

        activityLogRepository.save(commentLog);

        // when
        List<ActivityLog> result =
                activityLogRepository.findByActorIdAndEntityTypeNot(
                        actor.getId(),
                        ActivityEntityType.COMMENT
                );

        // then
        assertThat(result).isEmpty();
    }
}
