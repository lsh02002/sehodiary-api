package com.shop.sehodiary_api.web.controller.user;

import com.shop.sehodiary_api.repository.user.userDetails.CustomUserDetails;
import com.shop.sehodiary_api.service.exceptions.AccessDeniedException;
import com.shop.sehodiary_api.service.exceptions.NotAcceptableException;
import com.shop.sehodiary_api.service.user.UserService;
import com.shop.sehodiary_api.web.dto.user.LoginRequest;
import com.shop.sehodiary_api.web.dto.user.SignupRequest;
import com.shop.sehodiary_api.web.dto.user.UserInfoResponse;
import com.shop.sehodiary_api.web.dto.user.UserResponse;
import com.shop.sehodiary_api.web.dto.user.userLoginHist.UserLoginHistResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/sign-up")
    public ResponseEntity<UserResponse> signUp(@RequestBody SignupRequest signupRequest){
        return ResponseEntity.ok(userService.signUp(signupRequest));
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@RequestBody LoginRequest loginRequest, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse){
        List<Object> accessTokenAndRefreshTokenAndResponse = userService.login(loginRequest, httpServletRequest);
        httpServletResponse.addHeader("accessToken", accessTokenAndRefreshTokenAndResponse.get(0).toString());
        httpServletResponse.addHeader("refreshToken", accessTokenAndRefreshTokenAndResponse.get(1).toString());

        return ResponseEntity.ok((UserResponse) accessTokenAndRefreshTokenAndResponse.get(2));
    }

    @DeleteMapping("/logout")
    public ResponseEntity<UserResponse> logout(@AuthenticationPrincipal CustomUserDetails customUserDetails, HttpServletRequest request, HttpServletResponse response){
        return ResponseEntity.ok(userService.logout(customUserDetails.getEmail(), request, response));
    }

    @GetMapping("/info")
    public ResponseEntity<UserInfoResponse> getUserInfo(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        return ResponseEntity.ok(userService.getUserInfo(customUserDetails));
    }

    @GetMapping("/hist")
    public ResponseEntity<Page<UserLoginHistResponse>> getUserLoginHist(@AuthenticationPrincipal CustomUserDetails customUserDetails, Pageable pageable) {
        return ResponseEntity.ok(userService.getUserLoginHist(customUserDetails.getId(), pageable));
    }

    @GetMapping(value = "/entrypoint")
    public void entrypointException(@RequestParam(name = "accessToken", required = false) String token) {
        if (token==null) throw new NotAcceptableException("로그인(Jwt 토큰)이 필요합니다.", null);
        else throw new NotAcceptableException("로그인(Jwt 토큰)이 만료 되었습니다. 다시 로그인 하세요", null);
    }

    @GetMapping(value = "/access-denied")
    public void accessDeniedException(@RequestParam(name = "roles", required = false) String roles) {
        if(roles==null) throw new AccessDeniedException("권한이 설정되지 않았습니다.",null);
        else throw new AccessDeniedException("권한이 없습니다.", "시도한 유저의 권한 : "+roles);
    }

    @GetMapping("/test1")
    public ResponseEntity<Object> test1(@AuthenticationPrincipal CustomUserDetails customUserDetails){
        return ResponseEntity.ok(customUserDetails.toString());
    }

    @GetMapping("/test2")
    public ResponseEntity<String> test2(){
        return ResponseEntity.ok("Jwt 토큰이 상관없는 EntryPoint 테스트입니다.");
    }

    //관리자 모듈

    @PostMapping("/admin-login")
    public ResponseEntity<UserResponse> adminLogin(@RequestBody LoginRequest loginRequest, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse){
        List<Object> accessTokenAndRefreshTokenAndResponse = userService.adminLogin(loginRequest, httpServletRequest);
        httpServletResponse.addHeader("accessToken", accessTokenAndRefreshTokenAndResponse.get(0).toString());
        httpServletResponse.addHeader("refreshToken", accessTokenAndRefreshTokenAndResponse.get(1).toString());

        return ResponseEntity.ok((UserResponse) accessTokenAndRefreshTokenAndResponse.get(2));
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @GetMapping("/all-users-info")
    public ResponseEntity<Page<UserInfoResponse>> getAllUsersInfo(Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsersInfo(pageable));
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @GetMapping("/hist/{userId}")
    public ResponseEntity<Page<UserLoginHistResponse>> getUserLoginHistByAdmin(@PathVariable Long userId, Pageable pageable) {
        return ResponseEntity.ok(userService.getUserLoginHist(userId, pageable));
    }
}
