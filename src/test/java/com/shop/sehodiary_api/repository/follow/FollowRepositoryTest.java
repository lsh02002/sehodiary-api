package com.shop.sehodiary_api.repository.follow;

import com.shop.sehodiary_api.repository.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import jakarta.persistence.EntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(FollowRepositoryTest.JpaAuditingConfig.class)
class FollowRepositoryTest {
    private static final AtomicLong COUNTER = new AtomicLong();

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private EntityManager em;

    @TestConfiguration
    @EnableJpaAuditing
    static class JpaAuditingConfig {
    }

    @Test
    @DisplayName("existsByFollowerIdAndFollowingId - 팔로우 관계가 있으면 true 반환")
    void existsByFollowerIdAndFollowingId_true() {
        User follower = saveUser("follower");
        User following = saveUser("following");

        saveFollow(follower, following);
        em.flush();
        em.clear();

        boolean result = followRepository.existsByFollowerIdAndFollowingId(
                follower.getId(), following.getId()
        );

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("existsByFollowerIdAndFollowingId - 팔로우 관계가 없으면 false 반환")
    void existsByFollowerIdAndFollowingId_false() {
        User follower = saveUser("follower");
        User following = saveUser("following");

        em.flush();
        em.clear();

        boolean result = followRepository.existsByFollowerIdAndFollowingId(
                follower.getId(), following.getId()
        );

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("findByFollowerIdAndFollowingId - 팔로우 관계를 찾는다")
    void findByFollowerIdAndFollowingId() {
        User follower = saveUser("follower");
        User following = saveUser("following");

        Follow savedFollow = saveFollow(follower, following);
        em.flush();
        em.clear();

        Optional<Follow> result = followRepository.findByFollowerIdAndFollowingId(
                follower.getId(), following.getId()
        );

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(savedFollow.getId());
    }

    @Test
    @DisplayName("countByFollowerId - 내가 팔로우한 수를 반환한다")
    void countByFollowerId() {
        User me = saveUser("me");
        User user1 = saveUser("user1");
        User user2 = saveUser("user2");
        User user3 = saveUser("user3");

        saveFollow(me, user1);
        saveFollow(me, user2);
        saveFollow(me, user3);

        em.flush();
        em.clear();

        long count = followRepository.countByFollowerId(me.getId());

        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("countByFollowingId - 나를 팔로우한 수를 반환한다")
    void countByFollowingId() {
        User me = saveUser("me");
        User user1 = saveUser("user1");
        User user2 = saveUser("user2");

        saveFollow(user1, me);
        saveFollow(user2, me);

        em.flush();
        em.clear();

        long count = followRepository.countByFollowingId(me.getId());

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("findAllByFollowerIdOrderByIdDesc: 내가 팔로우하는 유저 목록을 최신순으로 조회한다")
    void findAllByFollowerIdOrderByIdDesc_success() {
        // given
        User me = saveUser("me");

        User user1 = saveUser("user1");

        User user2 = saveUser("user2");

        Follow follow1 = followRepository.save(Follow.builder()
                .follower(me)
                .following(user1)
                .build());

        Follow follow2 = followRepository.save(Follow.builder()
                .follower(me)
                .following(user2)
                .build());

        // when
        List<Follow> result = followRepository.findAllByFollowerIdOrderByIdDesc(me.getId());

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(follow2.getId());
        assertThat(result.get(1).getId()).isEqualTo(follow1.getId());

        assertThat(result.get(0).getFollower().getId()).isEqualTo(me.getId());
        assertThat(result.get(0).getFollowing().getNickname()).isEqualTo("user2");
        assertThat(result.get(1).getFollowing().getNickname()).isEqualTo("user1");
    }

    @Test
    @DisplayName("findAllByFollowingIdOrderByIdDesc: 나를 팔로우하는 유저 목록을 최신순으로 조회한다")
    void findAllByFollowingIdOrderByIdDesc_success() {
        // given
        User me = saveUser("me");

        User follower1 = saveUser("follower1");

        User follower2 = saveUser("follower2");

        Follow follow1 = followRepository.save(Follow.builder()
                .follower(follower1)
                .following(me)
                .build());

        Follow follow2 = followRepository.save(Follow.builder()
                .follower(follower2)
                .following(me)
                .build());

        // when
        List<Follow> result = followRepository.findAllByFollowingIdOrderByIdDesc(me.getId());

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(follow2.getId());
        assertThat(result.get(1).getId()).isEqualTo(follow1.getId());

        assertThat(result.get(0).getFollowing().getId()).isEqualTo(me.getId());
        assertThat(result.get(0).getFollower().getNickname()).isEqualTo("follower2");
        assertThat(result.get(1).getFollower().getNickname()).isEqualTo("follower1");
    }

    @Test
    @DisplayName("findAllByFollowerIdOrderByIdDesc: 팔로우한 유저가 없으면 빈 리스트를 반환한다")
    void findAllByFollowerIdOrderByIdDesc_empty() {
        // given
        User me = saveUser("me");

        // when
        List<Follow> result = followRepository.findAllByFollowerIdOrderByIdDesc(me.getId());

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllByFollowingIdOrderByIdDesc: 나를 팔로우한 유저가 없으면 빈 리스트를 반환한다")
    void findAllByFollowingIdOrderByIdDesc_empty() {
        // given
        User me = saveUser("me");

        // when
        List<Follow> result = followRepository.findAllByFollowingIdOrderByIdDesc(me.getId());

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("isFollowing - 팔로우 중이면 true 반환")
    void isFollowing_true() {
        User me = saveUser("me");
        User target = saveUser("target");

        saveFollow(me, target);

        em.flush();
        em.clear();

        boolean result = followRepository.isFollowing(me.getId(), target.getId());

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isFollowing - 팔로우 중이 아니면 false 반환")
    void isFollowing_false() {
        User me = saveUser("me");
        User target = saveUser("target");

        em.flush();
        em.clear();

        boolean result = followRepository.isFollowing(me.getId(), target.getId());

        assertThat(result).isFalse();
    }

    @Test
    void findUnfollowedUsers_test() {
        // given
        User me = saveUser("me");
        em.persist(me);

        User user1 = saveUser("user1");
        em.persist(user1);

        User user2 = saveUser("user2");
        em.persist(user2);

        User user3 = saveUser("user3");
        em.persist(user3);

        // me -> user1 팔로우
        Follow follow = new Follow();
        follow.setFollower(me);
        follow.setFollowing(user1);
        em.persist(follow);

        em.flush();
        em.clear();

        // when
        List<User> result = followRepository.findUnfollowedUsers(me.getId());

        // then
        // user1은 이미 팔로우했으므로 제외
        // me 본인도 제외
        assertThat(result)
                .extracting("nickname")
                .containsExactlyInAnyOrder("user2", "user3");
    }

    private User saveUser(String nickname) {
        User user = new User();
        user.setEmail("user@mail.com" + COUNTER.incrementAndGet());
        user.setPassword("12341234aaaa");
        user.setUserStatus("정상");
        user.setNickname(nickname);

        em.persist(user);
        return user;
    }

    private Follow saveFollow(User follower, User following) {
        Follow follow = Follow.builder()
                .follower(follower)
                .following(following)
                .build();

        em.persist(follow);
        return follow;
    }
}