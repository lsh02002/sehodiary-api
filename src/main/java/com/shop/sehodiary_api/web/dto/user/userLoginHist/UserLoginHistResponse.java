package com.shop.sehodiary_api.web.dto.user.userLoginHist;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLoginHistResponse {
    private Long histId;
    private Long userId;
    private String loginAt;
    private String clientIp;
    private String userAgent;
}
