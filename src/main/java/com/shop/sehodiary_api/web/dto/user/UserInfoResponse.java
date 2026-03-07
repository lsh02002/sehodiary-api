package com.shop.sehodiary_api.web.dto.user;

import lombok.*;

import java.util.List;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoResponse {
    private Long id;
    private String email;
    private String nickname;
    private List<String> profileImages;
}
