package com.shop.sehodiary_api.repository.user.refreshToken;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataRedisTest
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @AfterEach
    void tearDown() {
        refreshTokenRepository.deleteAll();
    }

    @Test
    @DisplayName("RefreshToken 저장 후 authId로 조회한다")
    void save_and_findById() {
        // given
        RefreshToken token = RefreshToken.builder()
                .authId("auth1")
                .refreshToken("refresh-token-123")
                .email("test@example.com")
                .build();

        // when
        refreshTokenRepository.save(token);

        // then
        Optional<RefreshToken> result = refreshTokenRepository.findById("auth1");

        assertThat(result).isPresent();
        assertThat(result.get().getAuthId()).isEqualTo("auth1");
        assertThat(result.get().getRefreshToken()).isEqualTo("refresh-token-123");
        assertThat(result.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("refreshToken으로 RefreshToken을 조회한다")
    void findByRefreshToken() {
        // given
        RefreshToken token = RefreshToken.builder()
                .authId("auth2")
                .refreshToken("refresh-token-456")
                .email("user@example.com")
                .build();

        refreshTokenRepository.save(token);

        // when
        Optional<RefreshToken> result = refreshTokenRepository.findByRefreshToken("refresh-token-456");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getAuthId()).isEqualTo("auth2");
        assertThat(result.get().getRefreshToken()).isEqualTo("refresh-token-456");
        assertThat(result.get().getEmail()).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("refreshToken으로 RefreshToken을 삭제한다")
    void deleteByRefreshToken() {
        // given
        RefreshToken token = RefreshToken.builder()
                .authId("auth3")
                .refreshToken("refresh-token-789")
                .email("delete@example.com")
                .build();

        refreshTokenRepository.save(token);

        // when
        RefreshToken savedToken = refreshTokenRepository.findByRefreshToken("refresh-token-789")
                .orElseThrow();

        refreshTokenRepository.delete(savedToken);

        // then
        Optional<RefreshToken> result =
                refreshTokenRepository.findByRefreshToken("refresh-token-789");

        assertThat(result).isEmpty();
    }
}