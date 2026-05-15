package com.shop.sehodiary_api.repository.user.refreshToken;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@RedisHash(value = "refresh_token")
public class RefreshToken {
    @Id
    private String authId;
    @Indexed
    private String refreshToken;
    @Indexed
    private String email;
}
