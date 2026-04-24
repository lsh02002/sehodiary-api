package com.shop.sehodiary_api.web.dto.follow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FollowUserResponse {
    private Long id;
    private Long userId;
    private String nickname;
    private String profileImage;
    private String introduction;
    private Long followerCounter;
    private Long followingCounter;
}