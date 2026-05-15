package com.shop.sehodiary_api.config.security;

import com.shop.sehodiary_api.config.redis.RedisUtil;
import com.shop.sehodiary_api.repository.user.refreshToken.RefreshToken;
import com.shop.sehodiary_api.repository.user.refreshToken.RefreshTokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RedisUtil redisUtil;
    private final UserDetailsService userDetailsService;

    @Value("${JWT_SECRET}")
    private String accessSecret;

    @Value("${JWT_REFRESH_SECRET}")
    private String refreshSecret;

    private SecretKey accessKey;
    private SecretKey refreshKey;

    private static final long ACCESS_TOKEN_EXPIRE_TIME = 1000L * 60 * 30;         // 30분
    private static final long REFRESH_TOKEN_EXPIRE_TIME = 1000L * 60 * 60 * 24 * 14; // 14일

    @PostConstruct
    public void init() {
        /*
         * application.yml / env 에 저장된 값이 Base64 인코딩된 비밀키라는 전제입니다.
         * 예:
         * JWT_SECRET=...
         * JWT_REFRESH_SECRET=...
         */
        this.accessKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(accessSecret));
        this.refreshKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(refreshSecret));
    }

    public String createAccessToken(String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ACCESS_TOKEN_EXPIRE_TIME);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(accessKey)
                .compact();
    }

    public String createRefreshToken(String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + REFRESH_TOKEN_EXPIRE_TIME);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(refreshKey)
                .compact();
    }

    public boolean validateAccessToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        if (redisUtil.hasKeyBlackList(token)) {
            log.warn("로그아웃된 access 토큰입니다.");
            return false;
        }

        try {
            parseAccessClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("만료된 access 토큰입니다.");
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("유효하지 않은 access 토큰입니다. message={}", e.getMessage());
        }

        return false;
    }

    public boolean validateRefreshToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            // 파싱 자체가 서명/형식/만료 검증 역할을 수행
            parseRefreshClaims(token);
        } catch (ExpiredJwtException e) {
            log.warn("만료된 refresh 토큰입니다.");
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("유효하지 않은 refresh 토큰입니다. message={}", e.getMessage());
            return false;
        }

        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByRefreshToken(token);
        return refreshToken.isPresent() && token.equals(refreshToken.get().getRefreshToken());
    }

    public Authentication getAuthentication(String accessToken) {
        String email = getEmailFromAccessToken(accessToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
    }

    public String getEmailFromAccessToken(String accessToken) {
        return parseAccessClaims(accessToken).getSubject();
    }

    public String getEmailFromRefreshToken(String refreshToken) {
        return parseRefreshClaims(refreshToken).getSubject();
    }

    private Claims parseAccessClaims(String token) {
        return Jwts.parser()
                .verifyWith(accessKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Claims parseRefreshClaims(String token) {
        return Jwts.parser()
                .verifyWith(refreshKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}