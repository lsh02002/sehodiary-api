package com.shop.sehodiary_api.web.mapper.follow;

import com.shop.sehodiary_api.config.s3.S3Address;
import com.shop.sehodiary_api.repository.follow.Follow;
import com.shop.sehodiary_api.web.dto.follow.FollowUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FollowMapper {
    private final S3Address s3Address;

    public FollowUserResponse toFollowingResponse(Follow follow) {
        return FollowUserResponse.builder()
                .id(follow.getId())
                .userId(follow.getFollowing().getId())
                .nickname(follow.getFollowing().getNickname())
                .profileImage(
                        follow.getFollowing().getProfileImages() != null &&
                                !follow.getFollowing().getProfileImages().isEmpty()
                                ? s3Address.siteAddress() +
                                follow.getFollowing().getProfileImages()
                                        .get(follow.getFollowing().getProfileImages().size() - 1)
                                        .getImageUrl()
                                : null
                )
                .introduction(follow.getFollowing().getIntroduction())
                .followerCounter((long) follow.getFollowing().getFollowerList().size())
                .followingCounter((long) follow.getFollowing().getFollowingList().size())
                .build();
    }

    public FollowUserResponse toFollowerResponse(Follow follow) {
        return FollowUserResponse.builder()
                .id(follow.getId())
                .userId(follow.getFollower().getId())
                .nickname(follow.getFollower().getNickname())
                .profileImage(
                        follow.getFollower().getProfileImages() != null &&
                                !follow.getFollower().getProfileImages().isEmpty()
                                ? s3Address.siteAddress() +
                                follow.getFollower().getProfileImages()
                                        .get(follow.getFollower().getProfileImages().size() - 1)
                                        .getImageUrl()
                                : null
                )
                .introduction(follow.getFollower().getIntroduction())
                .followerCounter((long) follow.getFollower().getFollowerList().size())
                .followingCounter((long) follow.getFollower().getFollowingList().size())
                .build();
    }
}
