package com.shop.sehodiary_api.repository.user.userLoginHist;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserLoginHistRepository extends JpaRepository<UserLoginHist, Long> {
    Page<UserLoginHist> findByUserId(Long userId, Pageable pageable);
}
