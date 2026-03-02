package com.shop.sehodiary_api.repository.user.refreshToken;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {
    Optional<RefreshToken> findByRefreshToken(String refreshToken);
    RefreshToken findByEmail(String email);
    void deleteByEmail(String email);
}
