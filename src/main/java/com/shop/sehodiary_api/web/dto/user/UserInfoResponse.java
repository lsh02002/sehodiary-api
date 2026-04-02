package com.shop.sehodiary_api.web.dto.user;

import lombok.*;

import java.util.List;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoResponse {
    private Long userId;
    private String email;
    private String nickname;
    private String profileImage;
    private String introduction;
    private Long followerCounter;
    private Long followingCounter;
}
