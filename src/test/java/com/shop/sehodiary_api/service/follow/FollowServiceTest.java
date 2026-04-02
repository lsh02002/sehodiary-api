package com.shop.sehodiary_api.service.follow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

import java.util.*;

import com.shop.sehodiary_api.repository.activity.function.SnapshotFunc;
import com.shop.sehodiary_api.repository.follow.Follow;
import com.shop.sehodiary_api.repository.follow.FollowRepository;
import com.shop.sehodiary_api.repository.user.User;
import com.shop.sehodiary_api.repository.user.UserRepository;
import com.shop.sehodiary_api.service.activelog.ActivityLogService;
import com.shop.sehodiary_api.service.exceptions.NotAcceptableException;
import com.shop.sehodiary_api.service.exceptions.NotFoundException;
import com.shop.sehodiary_api.web.dto.follow.FollowRelationshipResponse;
import com.shop.sehodiary_api.web.dto.follow.FollowUserResponse;
import com.shop.sehodiary_api.web.dto.user.UserInfoResponse;
import com.shop.sehodiary_api.web.mapper.follow.FollowMapper;
import com.shop.sehodiary_api.web.mapper.user.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

    @Mock
    private FollowRepository followRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FollowMapper followMapper;

    @Mock
    private SnapshotFunc snapshotFunc;

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private FollowService followService;

    private User follower;
    private User following;

    @BeforeEach
    void setUp() {
        follower = User.builder()
                .id(1L)
                .build();

        following = User.builder()
                .id(2L)
                .build();
    }

    @Nested
    @DisplayName("follow()")
    class FollowTest {

        @Test
        @DisplayName("자기 자신을 팔로우하려 하면 예외가 발생한다")
        void follow_fail_whenSelfFollow() {
            // when & then
            assertThatThrownBy(() -> followService.follow(1L, 1L))
                    .isInstanceOf(NotAcceptableException.class)
                    .extracting("detailMessage")
                    .isEqualTo("자기 자신은 팔로우할 수 없습니다.");

            verify(followRepository, never()).existsByFollowerIdAndFollowingId(any(), any());
            verify(userRepository, never()).findById(any());
            verify(followRepository, never()).save(any());
        }

        @Test
        @DisplayName("이미 팔로우 중이면 저장하지 않고 멱등 처리한다")
        void follow_noop_whenAlreadyFollowing() {
            // given
            when(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).thenReturn(true);

            // when
            followService.follow(1L, 2L);

            // then
            verify(followRepository).existsByFollowerIdAndFollowingId(1L, 2L);
            verify(userRepository, never()).findById(any());
            verify(followRepository, never()).save(any());
        }

        @Test
        @DisplayName("정상적으로 팔로우를 생성한다")
        void follow_success() {
            Follow follow = Follow.builder()
                    .id(100L)
                    .follower(follower)
                    .following(following)
                    .build();
            // given
            when(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).thenReturn(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(follower));
            when(userRepository.findById(2L)).thenReturn(Optional.of(following));

            when(followRepository.save(any(Follow.class))).thenReturn(follow);

            // when
            followService.follow(1L, 2L);

            // then
            ArgumentCaptor<Follow> captor = ArgumentCaptor.forClass(Follow.class);
            verify(followRepository).save(captor.capture());

            Follow savedFollow = captor.getValue();
            assertThat(savedFollow.getFollower()).isEqualTo(follower);
            assertThat(savedFollow.getFollowing()).isEqualTo(following);
        }

        @Test
        @DisplayName("팔로워 사용자가 없으면 예외가 발생한다")
        void follow_fail_whenFollowerNotFound() {
            // given
            when(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).thenReturn(false);
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> followService.follow(1L, 2L))
                    .isInstanceOf(NotFoundException.class);

            verify(userRepository).findById(1L);
            verify(userRepository, never()).findById(2L);
            verify(followRepository, never()).save(any());
        }

        @Test
        @DisplayName("팔로잉 대상 사용자가 없으면 예외가 발생한다")
        void follow_fail_whenFollowingNotFound() {
            // given
            when(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).thenReturn(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(follower));
            when(userRepository.findById(2L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> followService.follow(1L, 2L))
                    .isInstanceOf(NotFoundException.class);

            verify(userRepository).findById(1L);
            verify(userRepository).findById(2L);
            verify(followRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("unfollow()")
    class UnfollowTest {

        @Test
        @DisplayName("팔로우 관계가 존재하면 삭제한다")
        void unfollow_success() {
            // given
            Follow follow = Follow.builder()
                    .follower(follower)
                    .following(following)
                    .build();

            Map<String, Object> beforeFollow = new LinkedHashMap<>();

            when(userRepository.findById(1L)).thenReturn(Optional.of(follower));
            when(userRepository.findById(2L)).thenReturn(Optional.of(following));
            when(followRepository.findByFollowerIdAndFollowingId(1L, 2L))
                    .thenReturn(Optional.of(follow));
            when(snapshotFunc.snapshot(follow)).thenReturn(beforeFollow);

            // when
            followService.unfollow(1L, 2L);

            // then
            verify(followRepository).delete(follow);
        }

        @Test
        @DisplayName("팔로우 관계가 없으면 예외가 발생한다")
        void unfollow_fail_whenFollowNotExists() {
            // given
            Long followerId = 1L;
            Long followingId = 2L;

            User follower = User.builder()
                    .id(followerId)
                    .build();

            User following = User.builder()
                    .id(followingId)
                    .build();

            when(userRepository.findById(followerId)).thenReturn(Optional.of(follower));
            when(userRepository.findById(followingId)).thenReturn(Optional.of(following));
            when(followRepository.findByFollowerIdAndFollowingId(followerId, followingId))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> followService.unfollow(followerId, followingId))
                    .isInstanceOf(NotFoundException.class);

            verify(followRepository, never()).delete(any());
            verify(activityLogService, never()).log(any(), any(), any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("getRelationship()")
    class GetRelationshipTest {

        @Test
        @DisplayName("한쪽만 팔로우하면 isFollowing=true, isFollowedBy=false, isMutual=false 이어야 한다")
        void getRelationship_oneWay() {
            // given
            when(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).thenReturn(true);
            when(followRepository.existsByFollowerIdAndFollowingId(2L, 1L)).thenReturn(false);

            // when
            FollowRelationshipResponse response = followService.getRelationship(1L, 2L);

            // then
            assertThat(response.isFollowing()).isTrue();
            assertThat(response.isFollowedBy()).isFalse();
            assertThat(response.isMutual()).isFalse();
        }

        @Test
        @DisplayName("서로 팔로우하면 mutual=true 이어야 한다")
        void getRelationship_mutual() {
            // given
            when(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).thenReturn(true);
            when(followRepository.existsByFollowerIdAndFollowingId(2L, 1L)).thenReturn(true);

            // when
            FollowRelationshipResponse response = followService.getRelationship(1L, 2L);

            // then
            assertThat(response.isFollowing()).isTrue();
            assertThat(response.isFollowedBy()).isTrue();
            assertThat(response.isMutual()).isTrue();
        }

        @Test
        @DisplayName("서로 팔로우하지 않으면 모두 false")
        void getRelationship_none() {
            // given
            when(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).thenReturn(false);
            when(followRepository.existsByFollowerIdAndFollowingId(2L, 1L)).thenReturn(false);

            // when
            FollowRelationshipResponse response = followService.getRelationship(1L, 2L);

            // then
            assertThat(response.isFollowing()).isFalse();
            assertThat(response.isFollowedBy()).isFalse();
            assertThat(response.isMutual()).isFalse();
        }
    }

    @Nested
    @DisplayName("count 조회")
    class CountTest {

        @Test
        @DisplayName("팔로워 수를 조회한다")
        void getFollowerCount_success() {
            // given
            when(followRepository.countByFollowingId(2L)).thenReturn(10L);

            // when
            long count = followService.getFollowerCount(2L);

            // then
            assertThat(count).isEqualTo(10L);
        }

        @Test
        @DisplayName("팔로잉 수를 조회한다")
        void getFollowingCount_success() {
            // given
            when(followRepository.countByFollowerId(1L)).thenReturn(7L);

            // when
            long count = followService.getFollowingCount(1L);

            // then
            assertThat(count).isEqualTo(7L);
        }
    }

    @Nested
    @DisplayName("getFollowingList()")
    class GetFollowingListTest {

        @Test
        @DisplayName("내가 팔로우하는 목록을 조회한다")
        void getFollowingList_success() {
            // given
            Long userId = 1L;

            User me = User.builder()
                    .id(userId)
                    .nickname("me")
                    .build();

            User following1 = User.builder()
                    .id(2L)
                    .nickname("following1")
                    .build();

            User following2 = User.builder()
                    .id(3L)
                    .nickname("following2")
                    .build();

            Follow follow1 = Follow.builder()
                    .follower(me)
                    .following(following1)
                    .build();

            Follow follow2 = Follow.builder()
                    .follower(me)
                    .following(following2)
                    .build();

            FollowUserResponse response1 = FollowUserResponse.builder()
                    .userId(2L)
                    .nickname("following1")
                    .profileImage("https://cdn/f1.png")
                    .build();

            FollowUserResponse response2 = FollowUserResponse.builder()
                    .userId(3L)
                    .nickname("following2")
                    .profileImage("https://cdn/f2.png")
                    .build();

            given(followRepository.findAllByFollowerIdOrderByIdDesc(userId))
                    .willReturn(List.of(follow1, follow2));

            given(followMapper.toFollowingResponse(follow1)).willReturn(response1);
            given(followMapper.toFollowingResponse(follow2)).willReturn(response2);

            // when
            List<FollowUserResponse> result = followService.getFollowingList(userId);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(response1, response2);

            then(followRepository).should().findAllByFollowerIdOrderByIdDesc(userId);
            then(followMapper).should().toFollowingResponse(follow1);
            then(followMapper).should().toFollowingResponse(follow2);
            then(followMapper).should(never()).toFollowerResponse(org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("내가 팔로우하는 유저가 없으면 빈 리스트를 반환한다")
        void getFollowingList_empty() {
            // given
            Long userId = 1L;
            given(followRepository.findAllByFollowerIdOrderByIdDesc(userId))
                    .willReturn(Collections.emptyList());

            // when
            List<FollowUserResponse> result = followService.getFollowingList(userId);

            // then
            assertThat(result).isEmpty();

            then(followRepository).should().findAllByFollowerIdOrderByIdDesc(userId);
            then(followMapper).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("getFollowerList()")
    class GetFollowerListTest {

        @Test
        @DisplayName("나를 팔로우하는 목록을 조회한다")
        void getFollowerList_success() {
            // given
            Long userId = 1L;

            User me = User.builder()
                    .id(userId)
                    .nickname("me")
                    .build();

            User follower1 = User.builder()
                    .id(2L)
                    .nickname("follower1")
                    .build();

            User follower2 = User.builder()
                    .id(3L)
                    .nickname("follower2")
                    .build();

            Follow follow1 = Follow.builder()
                    .follower(follower1)
                    .following(me)
                    .build();

            Follow follow2 = Follow.builder()
                    .follower(follower2)
                    .following(me)
                    .build();

            FollowUserResponse response1 = FollowUserResponse.builder()
                    .userId(2L)
                    .nickname("follower1")
                    .profileImage("https://cdn/u1.png")
                    .build();

            FollowUserResponse response2 = FollowUserResponse.builder()
                    .userId(3L)
                    .nickname("follower2")
                    .profileImage("https://cdn/u2.png")
                    .build();

            given(followRepository.findAllByFollowingIdOrderByIdDesc(userId))
                    .willReturn(List.of(follow1, follow2));

            given(followMapper.toFollowerResponse(follow1)).willReturn(response1);
            given(followMapper.toFollowerResponse(follow2)).willReturn(response2);

            // when
            List<FollowUserResponse> result = followService.getFollowerList(userId);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(response1, response2);

            then(followRepository).should().findAllByFollowingIdOrderByIdDesc(userId);
            then(followMapper).should().toFollowerResponse(follow1);
            then(followMapper).should().toFollowerResponse(follow2);
            then(followMapper).should(never()).toFollowingResponse(org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("나를 팔로우하는 유저가 없으면 빈 리스트를 반환한다")
        void getFollowerList_empty() {
            // given
            Long userId = 1L;
            given(followRepository.findAllByFollowingIdOrderByIdDesc(userId))
                    .willReturn(Collections.emptyList());

            // when
            List<FollowUserResponse> result = followService.getFollowerList(userId);

            // then
            assertThat(result).isEmpty();

            then(followRepository).should().findAllByFollowingIdOrderByIdDesc(userId);
            then(followMapper).shouldHaveNoInteractions();
        }
    }

    @Nested
    class GetDiscoverUsersTest {
        private User user1;
        private User user2;
        private UserInfoResponse response1;
        private UserInfoResponse response2;

        @BeforeEach
        void setUp() {
            user1 = new User();
            user1.setId(2L);
            user1.setNickname("user1");

            user2 = new User();
            user2.setId(3L);
            user2.setNickname("user2");

            response1 = new UserInfoResponse(2L, "user1@mail.com", "user1", null, null, 0L, 0L);
            response2 = new UserInfoResponse(3L, "user2@mail.com", "user2", null, null, 0L, 0L);
        }

        @Test
        void getDiscoverUsers_성공() {
            // given
            Long userId = 1L;

            given(followRepository.findUnfollowedUsers(userId))
                    .willReturn(List.of(user1, user2));

            given(userMapper.toResponse(user1,  0L, 0L)).willReturn(response1);
            given(userMapper.toResponse(user2,  0L, 0L)).willReturn(response2);

            // when
            List<UserInfoResponse> result = followService.getDiscoverUsers(userId);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(response1, response2);

            verify(followRepository, times(1)).findUnfollowedUsers(userId);
            verify(userMapper, times(1)).toResponse(user1,0L, 0L);
            verify(userMapper, times(1)).toResponse(user2,0L, 0L);
        }

        @Test
        void getDiscoverUsers_추천유저가없으면_빈리스트반환() {
            // given
            Long userId = 1L;

            given(followRepository.findUnfollowedUsers(userId))
                    .willReturn(List.of());

            // when
            List<UserInfoResponse> result = followService.getDiscoverUsers(userId);

            // then
            assertThat(result).isEmpty();
            verify(followRepository, times(1)).findUnfollowedUsers(userId);
        }
    }
}
