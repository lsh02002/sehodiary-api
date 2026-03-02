package com.shop.sehodiary_api.web.dto.user;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignupResponse {
    private Long userId;
    private String nickname;
}
