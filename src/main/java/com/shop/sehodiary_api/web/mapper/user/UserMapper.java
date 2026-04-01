package com.shop.sehodiary_api.web.mapper.user;

import com.shop.sehodiary_api.config.s3.S3Address;
import com.shop.sehodiary_api.repository.follow.FollowRepository;
import com.shop.sehodiary_api.repository.user.User;
import com.shop.sehodiary_api.web.dto.user.UserInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@RequiredArgsConstructor
public class UserMapper {
    private final S3Address s3Address;

    public UserInfoResponse toResponse(User user, Long followerCount, Long followingCount) {
        return UserInfoResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImages(
                        user.getProfileImages() != null &&
                                !user.getProfileImages().isEmpty()
                                ? Collections.singletonList(s3Address.siteAddress() +
                                user.getProfileImages()
                                        .get(user.getProfileImages().size() - 1)
                                        .getImageUrl())
                                : null
                )
                .introduction(user.getIntroduction())
                .followerCounter(followerCount)
                .followingCounter(followingCount)
                .build();
    }
}
