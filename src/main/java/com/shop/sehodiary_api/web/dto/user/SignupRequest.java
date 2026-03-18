package com.shop.sehodiary_api.web.dto.user;

import lombok.*;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
public class SignupRequest {
    private String email;
    private String nickname;
    private String profileImage;
    private String password;
    private String passwordConfirm;
}
