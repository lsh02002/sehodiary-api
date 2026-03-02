package com.shop.sehodiary_api.web.dto.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class SignupRequest {
    private String email;
    private String nickname;
    private String profileImage;
    private String password;
    private String passwordConfirm;
}
