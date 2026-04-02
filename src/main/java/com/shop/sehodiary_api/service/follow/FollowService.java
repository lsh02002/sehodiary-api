package com.shop.sehodiary_api.service.follow;

import com.shop.sehodiary_api.repository.activity.ActivityAction;
import com.shop.sehodiary_api.repository.activity.ActivityEntityType;
import com.shop.sehodiary_api.repository.activity.function.SnapshotFunc;
import com.shop.sehodiary_api.repository.follow.Follow;
import com.shop.sehodiary_api.repository.follow.FollowRepository;
import com.shop.sehodiary_api.repository.user.User;
import com.shop.sehodiary_api.repository.user.UserRepository;
import com.shop.sehodiary_api.service.activelog.ActivityLogService;
import com.shop.sehodiary_api.service.exceptions.ConflictException;
import com.shop.sehodiary_api.service.exceptions.NotAcceptableException;
import com.shop.sehodiary_api.service.exceptions.NotFoundException;
import com.shop.sehodiary_api.web.dto.follow.FollowRelationshipResponse;
import com.shop.sehodiary_api.web.dto.follow.FollowUserResponse;
import com.shop.sehodiary_api.web.dto.user.UserInfoResponse;
import com.shop.sehodiary_api.web.dto.user.UserResponse;
import com.shop.sehodiary_api.web.mapper.follow.FollowMapper;
import com.shop.sehodiary_api.web.mapper.user.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final FollowMapper followMapper;
    private final SnapshotFunc snapshotFunc;

    private final ActivityLogService activityLogService;
    private final UserMapper userMapper;

    @Transactional
    public void follow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new NotAcceptableException("자기 자신은 팔로우할 수 없습니다.", followerId);
        }

        if (followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            return; // 이미 팔로우 중이면 멱등 처리
        }

        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new NotFoundException("해당 사용자가 없습니다.", followerId));
        User following = userRepository.findById(followingId)
                .orElseThrow(() -> new NotFoundException("해당 사용자가 없습니다.", followingId));

        Follow follow = followRepository.save(Follow.builder()
                        .follower(follower)
                        .following(following)
                .build());

        Object afterFollow = snapshotFunc.snapshot(follow);

        activityLogService.log(ActivityEntityType.FOLLOW, ActivityAction.CREATE, follow.getId(), follow.logMessage(), follower, null, afterFollow);
    }

    @Transactional
    public void unfollow(Long followerId, Long followingId) {
        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new NotFoundException("해당 사용자를 찾을 수 없습니다.", followerId));

        userRepository.findById(followingId)
                .orElseThrow(() -> new NotFoundException("해당 사용자를 찾을 수 없습니다.", followingId));

        Follow follow = followRepository.findByFollowerIdAndFollowingId(followerId, followingId)
                .orElseThrow(() -> new NotFoundException("해당 팔로우를 찾을 수 없습니다.", null));

        try {
            Object beforeFollow = snapshotFunc.snapshot(follow);

            activityLogService.log(
                    ActivityEntityType.FOLLOW,
                    ActivityAction.DELETE,
                    follow.getId(),
                    follow.logMessage(),
                    follower,
                    beforeFollow,
                    null
            );

            followRepository.delete(follow);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("해당 팔로우를 삭제할 수 없습니다.", follow.getId());
        }
    }

    @Transactional(readOnly = true)
    public FollowRelationshipResponse getRelationship(Long followerId, Long followingId) {
        boolean isFollowing = followRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
        boolean isFollowedBy = followRepository.existsByFollowerIdAndFollowingId(followingId, followerId);

        return new FollowRelationshipResponse(
                isFollowing,
                isFollowedBy,
                isFollowing && isFollowedBy
        );
    }

    @Transactional(readOnly = true)
    public long getFollowerCount(Long followerId) {
        return followRepository.countByFollowingId(followerId);
    }

    @Transactional(readOnly = true)
    public long getFollowingCount(Long followingId) {
        return followRepository.countByFollowerId(followingId);
    }

    @Transactional(readOnly = true)
    public List<FollowUserResponse> getFollowingList(Long userId) {
        return followRepository.findAllByFollowerIdOrderByIdDesc(userId)
                .stream().map(followMapper::toFollowingResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<FollowUserResponse> getFollowerList(Long userId) {
        return followRepository.findAllByFollowingIdOrderByIdDesc(userId)
                .stream().map(followMapper::toFollowerResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<UserInfoResponse> getDiscoverUsers(Long userId) {
        long followerCount = followRepository.countByFollowingId(userId);
        long followingCount = followRepository.countByFollowerId(userId);

        return followRepository.findUnfollowedUsers(userId)
                .stream().map(user->userMapper.toResponse(user, followerCount, followingCount)).toList();
    }
}