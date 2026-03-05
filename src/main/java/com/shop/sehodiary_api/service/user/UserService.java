package com.shop.sehodiary_api.service.user;

import com.shop.sehodiary_api.config.function.SnapshotFunc;
import com.shop.sehodiary_api.config.redis.RedisUtil;
import com.shop.sehodiary_api.config.security.JwtTokenProvider;
import com.shop.sehodiary_api.repository.activity.ActivityAction;
import com.shop.sehodiary_api.repository.activity.ActivityEntityType;
import com.shop.sehodiary_api.repository.user.User;
import com.shop.sehodiary_api.repository.user.UserRepository;
import com.shop.sehodiary_api.repository.user.refreshToken.RefreshToken;
import com.shop.sehodiary_api.repository.user.refreshToken.RefreshTokenRepository;
import com.shop.sehodiary_api.repository.user.userDetails.CustomUserDetails;
import com.shop.sehodiary_api.repository.user.userLoginHist.UserLoginHist;
import com.shop.sehodiary_api.repository.user.userLoginHist.UserLoginHistRepository;
import com.shop.sehodiary_api.repository.user.userRoles.Roles;
import com.shop.sehodiary_api.repository.user.userRoles.RolesRepository;
import com.shop.sehodiary_api.repository.user.userRoles.UserRoles;
import com.shop.sehodiary_api.repository.user.userRoles.UserRolesRepository;
import com.shop.sehodiary_api.service.activelog.ActivityLogService;
import com.shop.sehodiary_api.service.exceptions.AccessDeniedException;
import com.shop.sehodiary_api.service.exceptions.BadRequestException;
import com.shop.sehodiary_api.service.exceptions.ConflictException;
import com.shop.sehodiary_api.service.exceptions.NotFoundException;
import com.shop.sehodiary_api.web.dto.user.*;
import com.shop.sehodiary_api.web.dto.user.userLoginHist.UserLoginHistResponse;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final RolesRepository rolesRepository;
    private final UserRolesRepository userRolesRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisUtil redisUtil;
    private final UserLoginHistRepository userLoginHistRepository;

    private final PasswordEncoder passwordEncoder;

    private final ActivityLogService activityLogService;
    private final SnapshotFunc snapshotFunc;

    @PostConstruct
    private void insertRoleUserAndRoleAdminToNewDb(){
        //db를 새로 생성할 때 roles(ROLE_USER)초기값 생성
        Roles roleUser = rolesRepository.findByName("ROLE_USER");

        if(roleUser == null){
            Roles roles = rolesRepository.save(Roles.builder()
                    .name("ROLE_USER")
                    .build());

            Object afterRole = snapshotFunc.snapshot(roles);

            activityLogService.log(ActivityEntityType.ROLES, ActivityAction.CREATE, roles.getRolesId().longValue(), roles.logMessage(), null, null, afterRole);
        }

        //db를 새로 생성할 때 roles(ROLE_ADMIN)초기값 생성
        Roles roleAdmin = rolesRepository.findByName("ROLE_ADMIN");

        if(roleAdmin == null){
            Roles roles = rolesRepository.save(Roles.builder()
                    .name("ROLE_ADMIN")
                    .build());

            Object afterRole = snapshotFunc.snapshot(roles);

            activityLogService.log(ActivityEntityType.ROLES, ActivityAction.CREATE, roles.getRolesId().longValue(), roles.logMessage(), null, null, afterRole);
        }
    }

    @Transactional
    public UserResponse signUp(SignupRequest signupRequest){
        String email = signupRequest.getEmail();
        String nickname = signupRequest.getNickname();
        String password = signupRequest.getPassword();

        if(!email.matches("^[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*@[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*.[a-zA-Z]{2,3}$")){
            throw new BadRequestException("이메일을 정확히 입력해주세요.", email);
        }

        if(userRepository.existsByEmail(email)){
            throw new ConflictException("이미 입력하신 " + email + " 이메일로 가입된 계정이 있습니다.", email);
        }

        if(userRepository.existsByNickname(nickname)){
            throw new ConflictException("이미 입력하신 " + nickname + " 닉네임으로 가입된 계정이 있습니다.", nickname);
        }

        if(!password.matches("^(?=.*[a-zA-Z])(?=.*\\d)[a-zA-Z\\d]+$")
                ||!(password.length()>=8&&password.length()<=20)){
            throw new BadRequestException("비밀번호는 8자 이상 20자 이하 숫자와 영문소문자 조합 이어야 합니다.",password);
        }

        if(!signupRequest.getPasswordConfirm().equals(password)) {
            throw new BadRequestException("비밀번호와 비밀번호 확인이 같지 않습니다.","password : "+password+", password_confirm : "+signupRequest.getPasswordConfirm());
        }

        signupRequest.setPassword(passwordEncoder.encode(signupRequest.getPassword()));

        Roles roles = rolesRepository.findByName("ROLE_USER");

        User user = User.builder()
                .email(signupRequest.getEmail())
                .nickname(signupRequest.getNickname())
                .profileImage(signupRequest.getProfileImage())
                .password(signupRequest.getPassword())
                .userStatus("정상")
                .build();

        User savedUser = userRepository.save(user);

        Object afterUser = snapshotFunc.snapshot(savedUser);

        activityLogService.log(ActivityEntityType.USER, ActivityAction.CREATE, savedUser.getId(), savedUser.logMessage(), savedUser, null, afterUser);

        UserRoles userRoles = userRolesRepository.save(UserRoles.builder()
                .user(user)
                .roles(roles)
                .build());

        Object afterUserRoles = snapshotFunc.snapshot(userRoles);

        activityLogService.log(ActivityEntityType.USER_ROLES, ActivityAction.CREATE, savedUser.getId(), savedUser.logMessage(), savedUser, null, afterUserRoles);

        SignupResponse signupResponse = SignupResponse.builder()
                .userId(savedUser.getId())
                .build();

        return new UserResponse(HttpStatus.OK.value(), user.getEmail() + "님 회원 가입 완료 되었습니다.", signupResponse);
    }

    @Transactional
    public List<Object> login(LoginRequest request, HttpServletRequest httpServletRequest) {
        if(request.getEmail()==null||request.getPassword()==null){
            throw new BadRequestException("이메일이나 비밀번호 값이 비어있습니다.","email : "+request.getEmail()+", password : "+request.getPassword());
        }
        User user;

        if(request.getEmail().matches("^[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*@[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*.[a-zA-Z]{2,3}$")) {
            user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new NotFoundException("입력하신 이메일의 계정을 찾을 수 없습니다.", request.getEmail()));
        } else {
            throw new BadRequestException("이메일이나 비밀번호가 잘못 입력되었습니다.", null);
        }
        String p1 = user.getPassword();

        if(!passwordEncoder.matches(request.getPassword(), p1)){
            throw new BadRequestException("이메일이나 비밀번호가 잘못 입력되었습니다.", null);
        }

        if (user.getUserStatus().equals("탈퇴")) {
            throw new AccessDeniedException("탈퇴한 계정입니다.", request.getEmail());
        }

        List<String> roles = user.getUserRoles().stream()
                .map(UserRoles::getRoles).map(Roles::getName).toList();

        userLoginHistRepository.save(UserLoginHist.builder()
                .user(user)
                .clientIp(getClientIP(httpServletRequest))
                .userAgent(getUserAgent(httpServletRequest))
                .loginAt(LocalDateTime.now())
                .build());

        SignupResponse signupResponse = SignupResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .build();

        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());

        RefreshToken newToken = RefreshToken.builder()
                .authId(user.getId().toString())
                .refreshToken(newRefreshToken)
                .email(user.getEmail())
                .build();

        refreshTokenRepository.save(newToken);

        UserResponse authResponse = new UserResponse(HttpStatus.OK.value(), "로그인에 성공 하였습니다.", signupResponse);

        return Arrays.asList(jwtTokenProvider.createAccessToken(user.getEmail()), newRefreshToken, authResponse);
    }

    public UserInfoResponse getUserInfo(CustomUserDetails customUserDetails) {
        return UserInfoResponse.builder()
                .userId(customUserDetails.getId())
                .nickname(customUserDetails.getNickname())
                .build();
    }

    @Transactional
    public UserResponse logout(String email, HttpServletRequest request, HttpServletResponse response){
        String refreshToken = request.getHeader("refreshToken");
        String accessToken = request.getHeader("accessToken");

        if(email == null) {
            throw new BadRequestException("유저 정보가 비어있습니다.", null);
        }

        refreshTokenRepository.deleteByRefreshToken(refreshToken);

        if(jwtTokenProvider.validateToken(accessToken)) {
            redisUtil.setBlackList(accessToken, "accessToken", 30);
        }

        return new UserResponse(HttpStatus.OK.value(), "로그아웃에 성공 하였습니다.", null);
    }

    @Transactional
    public UserResponse withdrawal(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new NotFoundException("계정을 찾을 수 없습니다. 다시 로그인 해주세요.", email));

        Object beforeUser = snapshotFunc.snapshot(user);

        if (user.getUserStatus().equals("탈퇴")) {
            throw new BadRequestException("이미 탈퇴처리된 회원 입니다.", email);
        }
        user.setUserStatus("탈퇴");
        user.setDeletedAt(LocalDateTime.now());

        Object afterUser = snapshotFunc.snapshot(user);

        activityLogService.log(ActivityEntityType.USER, ActivityAction.DELETE, user.getId(), user.logMessage(), user, beforeUser, afterUser);

        return new UserResponse(200, "회원탈퇴 완료 되었습니다.", user.getNickname());
    }

    public Page<UserLoginHistResponse> getUserLoginHist(Long userId, Pageable pageable) {
        return userLoginHistRepository.findByUserId(userId, pageable)
                .map(hist->UserLoginHistResponse.builder()
                        .histId(hist.getId())
                        .userId(hist.getUser().getId())
                        .loginAt(hist.getLoginAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                        .clientIp(hist.getClientIp())
                        .userAgent(hist.getUserAgent())
                        .build());
    }

    @Transactional
    public List<Object> adminLogin(LoginRequest request, HttpServletRequest httpServletRequest) {
        if(request.getEmail()==null||request.getPassword()==null){
            throw new BadRequestException("이메일이나 비밀번호 값이 비어있습니다.","email : "+request.getEmail()+", password : "+request.getPassword());
        }
        User user;

        if(request.getEmail().matches("^[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*@[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*.[a-zA-Z]{2,3}$")) {
            user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new NotFoundException("입력하신 이메일의 계정을 찾을 수 없습니다.", request.getEmail()));
        } else {
            throw new BadRequestException("이메일이나 비밀번호가 잘못 입력되었습니다.", null);
        }
        String p1 = user.getPassword();

        if(!passwordEncoder.matches(request.getPassword(), p1)){
            throw new BadRequestException("이메일이나 비밀번호가 잘못 입력되었습니다.", null);
        }

        if (user.getUserStatus().equals("탈퇴")) {
            throw new AccessDeniedException("탈퇴한 계정입니다.", request.getEmail());
        }

        List<String> roles = user.getUserRoles().stream()
                .map(UserRoles::getRoles).map(Roles::getName).toList();

        if(!roles.contains("ROLE_ADMIN")){
            throw new BadRequestException("관리자 권한이 없습니다.", request.getEmail());
        }

        userLoginHistRepository.save(UserLoginHist.builder()
                .user(user)
                .clientIp(getClientIP(httpServletRequest))
                .userAgent(getUserAgent(httpServletRequest))
                .loginAt(LocalDateTime.now())
                .build());

        SignupResponse signupResponse = SignupResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .build();

        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());

        RefreshToken newToken = RefreshToken.builder()
                .authId(user.getId().toString())
                .refreshToken(newRefreshToken)
                .email(user.getEmail())
                .build();

        refreshTokenRepository.save(newToken);

        UserResponse authResponse = new UserResponse(HttpStatus.OK.value(), "로그인에 성공 하였습니다.", signupResponse);

        return Arrays.asList(jwtTokenProvider.createAccessToken(user.getEmail()), newRefreshToken, authResponse);
    }

    public Page<UserInfoResponse> getAllUsersInfo(Pageable pageable){
        return userRepository.findAll(pageable)
                .map(user->UserInfoResponse.builder()
                        .userId(user.getId())
                        .nickname(user.getNickname())
                        .build());
    }

    private static String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null) {
            ip = request.getRemoteAddr();
        }

        return ip;
    }

    private static String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }
}
