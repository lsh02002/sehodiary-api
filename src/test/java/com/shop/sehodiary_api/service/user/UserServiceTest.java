package com.shop.sehodiary_api.service.user;

import com.shop.sehodiary_api.config.redis.RedisUtil;
import com.shop.sehodiary_api.config.s3.S3Address;
import com.shop.sehodiary_api.config.security.JwtTokenProvider;
import com.shop.sehodiary_api.repository.activity.ActivityAction;
import com.shop.sehodiary_api.repository.activity.ActivityEntityType;
import com.shop.sehodiary_api.repository.activity.function.SnapshotFunc;
import com.shop.sehodiary_api.repository.comment.CommentCacheRepository;
import com.shop.sehodiary_api.repository.diary.DiaryCacheRepository;
import com.shop.sehodiary_api.repository.diaryImage.DiaryImage;
import com.shop.sehodiary_api.repository.follow.Follow;
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
import com.shop.sehodiary_api.service.profileimage.ProfileImageService;
import com.shop.sehodiary_api.web.dto.user.*;
import com.shop.sehodiary_api.web.mapper.user.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RolesRepository rolesRepository;

    @Mock
    private UserRolesRepository userRolesRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @Mock
    private SnapshotFunc snapshotFunc;

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private UserLoginHistRepository userLoginHistRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private S3Address s3Address;

    @Mock
    private RedisUtil redisUtil;

    @Mock
    private ProfileImageService profileImageService;

    @Mock
    private DiaryCacheRepository diaryCacheRepository;

    @Mock
    private CommentCacheRepository commentCacheRepository;

    @Nested
    class SignupTest {

        @Test
        @DisplayName("회원 가입 성공")
        void signUp_success() {
            SignupRequest request = SignupRequest.builder()
                    .email("test001@sample.com")
                    .nickname("test001")
                    .password("12341234aaaa")
                    .passwordConfirm("12341234aaaa")
                    .build();

            Roles roleUser = Roles.builder()
                    .name("ROLE_USER")
                    .build();

            User savedUser = User.builder()
                    .id(1L)
                    .email("test001@sample.com")
                    .nickname("test001")
                    .password("encodedPassword")
                    .userStatus("정상")
                    .build();

            when(userRepository.existsByEmail("test001@sample.com")).thenReturn(false);
            when(userRepository.existsByNickname("test001")).thenReturn(false);
            when(passwordEncoder.encode("12341234aaaa")).thenReturn("encodedPassword");
            when(rolesRepository.findByName("ROLE_USER")).thenReturn(roleUser);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(snapshotFunc.snapshot(any())).thenReturn(new HashMap<>());
            when(userRolesRepository.save(any(UserRoles.class)))
                    .thenReturn(UserRoles.builder()
                            .userRolesId(1)
                            .user(savedUser)
                            .roles(roleUser)
                            .build());

            UserResponse response = userService.signUp(request);

            assertThat(response).isNotNull();
            assertThat(response.getData()).isInstanceOf(SignupResponse.class);

            SignupResponse signupResponse = (SignupResponse) response.getData();
            assertThat(signupResponse.getUserId()).isEqualTo(1L);
            assertThat(signupResponse.getNickname()).isEqualTo("test001");
        }

        @Test
        @DisplayName("회원 가입 실패 - 이메일 중복")
        void signUp_fail_duplicateEmail() {
            SignupRequest request = SignupRequest.builder()
                    .email("test001@sample.com")
                    .nickname("test001")
                    .password("12341234aaaa")
                    .passwordConfirm("12341234aaaa")
                    .build();

            when(userRepository.existsByEmail("test001@sample.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.signUp(request))
                    .isInstanceOf(ConflictException.class)
                    .extracting("detailMessage")
                    .isEqualTo("이미 입력하신 " + request.getEmail() + " 이메일로 가입된 계정이 있습니다.");
        }

        @Test
        @DisplayName("회원 가입 실패 - 닉네임 중복")
        void signUp_fail_duplicateNickname() {
            SignupRequest request = SignupRequest.builder()
                    .email("test001@sample.com")
                    .nickname("test001")
                    .password("12341234aaaa")
                    .passwordConfirm("12341234aaaa")
                    .build();

            when(userRepository.existsByEmail("test001@sample.com")).thenReturn(false);
            when(userRepository.existsByNickname("test001")).thenReturn(true);

            assertThatThrownBy(() -> userService.signUp(request))
                    .isInstanceOf(ConflictException.class)
                    .extracting("detailMessage")
                    .isEqualTo("이미 입력하신 " + request.getNickname() + " 닉네임으로 가입된 계정이 있습니다.");
        }

        @Test
        @DisplayName("회원 가입 실패 - 비밀번호 확인 불일치")
        void signUp_fail_passwordConfirmMismatch() {
            SignupRequest request = SignupRequest.builder()
                    .email("test001@sample.com")
                    .nickname("test001")
                    .password("12341234aaaa")
                    .passwordConfirm("different123")
                    .build();

            when(userRepository.existsByEmail("test001@sample.com")).thenReturn(false);
            when(userRepository.existsByNickname("test001")).thenReturn(false);

            assertThatThrownBy(() -> userService.signUp(request))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("detailMessage")
                    .isEqualTo("비밀번호와 비밀번호 확인이 같지 않습니다.");
        }

        @Test
        @DisplayName("회원 가입 실패 - 닉네임 형식 오류")
        void signUp_fail_invalidNickname() {
            SignupRequest request = SignupRequest.builder()
                    .email("test001@sample.com")
                    .nickname("1abc") // 핵심: 첫 글자가 숫자 → 실패
                    .password("12341234aaaa")
                    .passwordConfirm("12341234aaaa")
                    .build();

            assertThatThrownBy(() -> userService.signUp(request))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("detailMessage")
                    .isEqualTo("닉네임은 첫번째 영문자이고 나머지는 영문자 숫자 조합입니다.");
        }
    }

    @Nested
    class LoginTest {

        @Test
        @DisplayName("로그인 성공")
        void login_success() {
            LoginRequest request = LoginRequest.builder()
                    .email("test001@sample.com")
                    .password("12341234aaaa")
                    .build();

            Roles roleUser = Roles.builder()
                    .name("ROLE_USER")
                    .build();

            User user = User.builder()
                    .id(1L)
                    .email("test001@sample.com")
                    .nickname("test001")
                    .password("encodedPassword")
                    .userStatus("정상")
                    .userRoles(List.of(
                            UserRoles.builder()
                                    .roles(roleUser)
                                    .build()
                    ))
                    .build();

            when(userRepository.findByEmail("test001@sample.com"))
                    .thenReturn(Optional.of(user));
            when(passwordEncoder.matches(request.getPassword(), user.getPassword()))
                    .thenReturn(true);

            when(jwtTokenProvider.createRefreshToken(user.getEmail()))
                    .thenReturn("new-refresh-token");

            when(userLoginHistRepository.save(any(UserLoginHist.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            when(refreshTokenRepository.save(any(RefreshToken.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            List<Object> objects = userService.login(request, httpServletRequest);

            assertThat(objects).isNotNull();
            assertThat(objects).isNotEmpty();
        }

        @Test
        @DisplayName("로그인 실패 - 빈 이메일")
        void login_fail_emptyEmail() {
            LoginRequest request = LoginRequest.builder()
                    .email("")
                    .password("12341234aaaa")
                    .build();

            assertThatThrownBy(() -> userService.login(request, httpServletRequest))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("detailMessage")
                    .isEqualTo("이메일이나 비밀번호 값이 비어있습니다.");
        }

        @Test
        @DisplayName("로그인 실패 - 잘못된 이메일 형식")
        void login_fail_invalidEmail() {
            LoginRequest request = LoginRequest.builder()
                    .email("123456789")
                    .password("12341234aaaa")
                    .build();

            assertThatThrownBy(() -> userService.login(request, httpServletRequest))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("detailMessage")
                    .isEqualTo("이메일이나 비밀번호가 잘못 입력되었습니다.");
        }

        @Test
        @DisplayName("로그인 실패 - 비밀번호가 틀렸을 때")
        void login_fail_mismatchPassword() {
            LoginRequest request = LoginRequest.builder()
                    .email("test001@sample.com")
                    .password("12341234aaaa")
                    .build();

            Roles roleUser = Roles.builder()
                    .name("ROLE_USER")
                    .build();

            User user = User.builder()
                    .id(1L)
                    .email("test001@sample.com")
                    .nickname("test001")
                    .password("encodedPassword")
                    .userStatus("정상")
                    .userRoles(List.of(
                            UserRoles.builder()
                                    .roles(roleUser)
                                    .build()
                    ))
                    .build();

            when(userRepository.findByEmail("test001@sample.com"))
                    .thenReturn(Optional.of(user));

            when(passwordEncoder.matches(request.getPassword(), user.getPassword()))
                    .thenReturn(false);

            assertThatThrownBy(() -> userService.login(request, httpServletRequest))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("detailMessage")
                    .isEqualTo("이메일이나 비밀번호가 잘못 입력되었습니다.");
        }

        @Test
        @DisplayName("로그인 - 이미 탈퇴한 계정")
        void login_fail_alreadyWithdrawal() {
            LoginRequest request = LoginRequest.builder()
                    .email("test001@sample.com")
                    .password("12341234aaaa")
                    .build();

            Roles roleUser = Roles.builder()
                    .name("ROLE_USER")
                    .build();

            User user = User.builder()
                    .id(1L)
                    .email("test001@sample.com")
                    .nickname("test001")
                    .password("encodedPassword")
                    .userStatus("탈퇴")
                    .userRoles(List.of(
                            UserRoles.builder()
                                    .roles(roleUser)
                                    .build()
                    ))
                    .build();

            when(userRepository.findByEmail("test001@sample.com"))
                    .thenReturn(Optional.of(user));

            when(passwordEncoder.matches(request.getPassword(), user.getPassword()))
                    .thenReturn(true);

            assertThatThrownBy(() -> userService.login(request, httpServletRequest))
                    .isInstanceOf(AccessDeniedException.class)
                    .extracting("detailMessage")
                    .isEqualTo("탈퇴한 계정입니다.");
        }

        @Test
        @DisplayName("로그인 - 사용자 권한이 없을 때")
        void login_fail_hasNoAuthority() {
            LoginRequest request = LoginRequest.builder()
                    .email("test001@sample.com")
                    .password("12341234aaaa")
                    .build();

            Roles roleUser = Roles.builder()
                    .name("ROLE_UNKNOWN")
                    .build();

            User user = User.builder()
                    .id(1L)
                    .email("test001@sample.com")
                    .nickname("test001")
                    .password("encodedPassword")
                    .userStatus("정상")
                    .userRoles(List.of(
                            UserRoles.builder()
                                    .roles(roleUser)
                                    .build()
                    ))
                    .build();

            when(userRepository.findByEmail("test001@sample.com"))
                    .thenReturn(Optional.of(user));

            when(passwordEncoder.matches(request.getPassword(), user.getPassword()))
                    .thenReturn(true);

            assertThatThrownBy(() -> userService.login(request, httpServletRequest))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("detailMessage")
                    .isEqualTo("사용자 권한이 없습니다.");
        }
    }

    @Nested
    class GetUserInfoTest {

        @Test
        @DisplayName("내 정보 조회 성공 - 삭제되지 않은 프로필 이미지들만 반환한다")
        void getUserInfo_success() {
            // given
            Long userId = 1L;

            CustomUserDetails customUserDetails = CustomUserDetails.builder()
                    .id(userId)
                    .build();

            DiaryImage activeImage1 = DiaryImage.builder()
                    .imageUrl("/profile/user1.png")
                    .deleted(false)
                    .build();

            DiaryImage deletedImage = DiaryImage.builder()
                    .imageUrl("/profile/deleted.png")
                    .deleted(true)
                    .build();

            DiaryImage activeImage2 = DiaryImage.builder()
                    .imageUrl("/profile/user2.png")
                    .deleted(false)
                    .build();

            User user = User.builder()
                    .id(userId)
                    .email("test001@sample.com")
                    .nickname("test001")
                    .profileImages(List.of(activeImage1, deletedImage, activeImage2))
                    .followerList(List.of())
                    .followingList(List.of())
                    .build();

            UserInfoResponse userInfoResponse = UserInfoResponse.builder()
                    .userId(userId)
                    .email(user.getEmail())
                    .nickname(user.getNickname())
                    .profileImage("https://cdn.sample.com/profile/user2.png")
                    .build();

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(userMapper.toResponse(user, 0L, 0L)).willReturn(userInfoResponse);

            // when
            UserInfoResponse response = userService.getUserInfo(customUserDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getUserId()).isEqualTo(1L);
            assertThat(response.getEmail()).isEqualTo("test001@sample.com");
            assertThat(response.getNickname()).isEqualTo("test001");
            assertThat(response.getProfileImage()).isEqualTo("https://cdn.sample.com/profile/user2.png");

            verify(userRepository).findById(userId);
            verify(userMapper).toResponse(user, 0L, 0L);
        }

        @Test
        @DisplayName("내 정보 조회 성공 - 프로필 이미지가 없으면 null을 반환한다")
        void getUserInfo_success_emptyProfileImages() {
            // given
            Long userId = 1L;

            CustomUserDetails customUserDetails = CustomUserDetails.builder()
                    .id(userId)
                    .build();

            User user = User.builder()
                    .id(userId)
                    .email("test001@sample.com")
                    .nickname("test001")
                    .profileImages(List.of())
                    .followerList(List.of())
                    .followingList(List.of())
                    .build();

            UserInfoResponse userInfoResponse = UserInfoResponse.builder()
                    .userId(userId)
                    .email(user.getEmail())
                    .nickname(user.getNickname())
                    .profileImage(null)
                    .build();

            given(userRepository.findById(userId))
                    .willReturn(Optional.of(user));

            given(userMapper.toResponse(user, 0L, 0L))
                    .willReturn(userInfoResponse);

            // when
            UserInfoResponse response = userService.getUserInfo(customUserDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getProfileImage()).isNull();
        }

        @Test
        @DisplayName("내 정보 조회 실패 - 사용자가 없으면 NotFoundException 발생")
        void getUserInfo_fail_userNotFound() {
            // given
            Long userId = 999L;

            CustomUserDetails customUserDetails = CustomUserDetails.builder()
                    .id(userId)
                    .build();

            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.getUserInfo(customUserDetails))
                    .isInstanceOf(NotFoundException.class)
                    .extracting("detailMessage")
                    .isEqualTo("해당 사용자를 찾을 수 없습니다.");
        }
    }

    @Nested
    class getOtherUserInfoTest {
        private User loginUser;
        private User otherUser;
        private UserInfoResponse userInfoResponse;

        @BeforeEach
        void setUp() {
            loginUser = User.builder()
                    .id(1L)
                    .followerList(new ArrayList<>())
                    .followingList(new ArrayList<>())
                    .build();

            otherUser = User.builder()
                    .id(2L)
                    .followerList(new ArrayList<>())
                    .followingList(new ArrayList<>())
                    .build();

            otherUser.getFollowerList().add(new Follow());
            otherUser.getFollowerList().add(new Follow());
            otherUser.getFollowingList().add(new Follow());

            userInfoResponse = UserInfoResponse.builder()
                    .userId(2L)
                    .followerCounter(2L)
                    .followingCounter(1L)
                    .build();
        }

        @Test
        @DisplayName("다른 사용자 정보 조회 성공")
        void getOtherUserInfo_success() {
            // given
            when(userRepository.findById(1L)).thenReturn(Optional.of(loginUser));
            when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));
            when(userMapper.toResponse(eq(otherUser), eq(2L), eq(1L)))
                    .thenReturn(userInfoResponse);

            // when
            UserInfoResponse result = userService.getOtherUserInfo(1L, 2L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(2L);
            assertThat(result.getFollowerCounter()).isEqualTo(2L);
            assertThat(result.getFollowingCounter()).isEqualTo(1L);

            verify(userRepository).findById(1L);
            verify(userRepository).findById(2L);
            verify(userMapper).toResponse(otherUser, 2L, 1L);
        }

        @Test
        @DisplayName("로그인 사용자가 없으면 예외 발생")
        void getOtherUserInfo_userNotFound() {
            // given
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.getOtherUserInfo(1L, 2L))
                    .isInstanceOf(NotFoundException.class)
                    .extracting("detailMessage")
                    .isEqualTo("해당 사용자를 찾을 수 없습니다.");
        }

        @Test
        @DisplayName("조회 대상 사용자가 없으면 예외 발생")
        void getOtherUserInfo_otherUserNotFound() {
            // given
            when(userRepository.findById(1L)).thenReturn(Optional.of(loginUser));
            when(userRepository.findById(2L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.getOtherUserInfo(1L, 2L))
                    .isInstanceOf(NotFoundException.class)
                    .extracting("detailMessage")
                    .isEqualTo("해당 사용자를 찾을 수 없습니다.");
        }
    }

    @Nested
    class LogoutTest {

        @Test
        @DisplayName("로그아웃 성공 - accessToken이 유효하면 refreshToken 삭제 후 블랙리스트 등록")
        void logout_success_validAccessToken() {
            // given
            String email = "test001@sample.com";
            String refreshToken = "refresh-token";
            String accessToken = "access-token";

            RefreshToken savedToken = RefreshToken.builder()
                    .authId("1")
                    .refreshToken(refreshToken)
                    .email(email)
                    .build();

            given(httpServletRequest.getHeader("refreshToken")).willReturn(refreshToken);
            given(httpServletRequest.getHeader("accessToken")).willReturn(accessToken);
            given(refreshTokenRepository.findByRefreshToken(refreshToken))
                    .willReturn(Optional.of(savedToken));
            given(jwtTokenProvider.validateAccessToken(accessToken)).willReturn(true);

            // when
            UserResponse result = userService.logout(email, httpServletRequest);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getMessage()).isEqualTo("로그아웃에 성공 하였습니다.");
            assertThat(result.getData()).isNull();

            verify(refreshTokenRepository).findByRefreshToken(refreshToken);
            verify(refreshTokenRepository).delete(savedToken);
            verify(jwtTokenProvider).validateAccessToken(accessToken);
            verify(redisUtil).setBlackList(accessToken, "accessToken", 30);
        }

        @Test
        @DisplayName("로그아웃 성공 - accessToken이 유효하지 않으면 refreshToken만 삭제하고 블랙리스트 등록은 하지 않는다")
        void logout_success_invalidAccessToken() {
            // given
            String email = "test001@sample.com";
            String refreshToken = "refresh-token";
            String accessToken = "invalid-access-token";

            RefreshToken savedToken = RefreshToken.builder()
                    .authId("1")
                    .refreshToken(refreshToken)
                    .email(email)
                    .build();

            given(httpServletRequest.getHeader("refreshToken")).willReturn(refreshToken);
            given(httpServletRequest.getHeader("accessToken")).willReturn(accessToken);
            given(refreshTokenRepository.findByRefreshToken(refreshToken))
                    .willReturn(Optional.of(savedToken));
            given(jwtTokenProvider.validateAccessToken(accessToken)).willReturn(false);

            // when
            UserResponse result = userService.logout(email, httpServletRequest);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getMessage()).isEqualTo("로그아웃에 성공 하였습니다.");
            assertThat(result.getData()).isNull();

            verify(refreshTokenRepository).delete(savedToken);
            verify(jwtTokenProvider).validateAccessToken(accessToken);
            verify(redisUtil, never()).setBlackList(anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt());
        }

        @Test
        @DisplayName("로그아웃 실패 - email이 null이면 BadRequestException 발생")
        void logout_fail_emailIsNull() {
            // given
            String email = null;

            // when & then
            assertThatThrownBy(() -> userService.logout(email, httpServletRequest))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("detailMessage")
                    .isEqualTo("유저 정보가 비어있습니다.");

            verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
            verify(jwtTokenProvider, never()).validateAccessToken(anyString());
            verify(redisUtil, never()).setBlackList(anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt());
        }
    }

    @Nested
    class WithdrawalTest {

        @Test
        @DisplayName("회원 탈퇴 성공")
        void withdrawal_success() {
            // given
            String email = "test001@sample.com";

            User user = User.builder()
                    .id(1L)
                    .email(email)
                    .nickname("test001")
                    .userStatus("정상")
                    .build();

            given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
            given(snapshotFunc.snapshot(user))
                    .willReturn(new HashMap<>());

            // when
            UserResponse response = userService.withdrawal(email);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getMessage()).isEqualTo("회원탈퇴 완료 되었습니다.");
            assertThat(response.getData()).isEqualTo("test001");

            assertThat(user.getUserStatus()).isEqualTo("탈퇴");
            assertThat(user.getDeletedAt()).isNotNull();

            verify(userRepository).findByEmail(email);
            verify(snapshotFunc, times(2)).snapshot(user);
            verify(activityLogService).log(
                    eq(ActivityEntityType.USER),
                    eq(ActivityAction.DELETE),
                    eq(user.getId()),
                    eq(user.logMessage()),
                    eq(user),
                    eq(new HashMap<>()),
                    eq(new HashMap<>())
            );
        }

        @Test
        @DisplayName("회원 탈퇴 실패 - 사용자를 찾을 수 없음")
        void withdrawal_fail_userNotFound() {
            // given
            String email = "notfound@sample.com";
            given(userRepository.findByEmail(email)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.withdrawal(email))
                    .isInstanceOf(NotFoundException.class)
                    .extracting("detailMessage")
                    .isEqualTo("계정을 찾을 수 없습니다. 다시 로그인 해주세요.");

            verify(snapshotFunc, never()).snapshot(any());
            verify(activityLogService, never()).log(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("회원 탈퇴 실패 - 이미 탈퇴한 회원")
        void withdrawal_fail_alreadyWithdrawn() {
            // given
            String email = "test001@sample.com";

            User user = User.builder()
                    .id(1L)
                    .email(email)
                    .nickname("test001")
                    .userStatus("탈퇴")
                    .build();

            given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
            given(snapshotFunc.snapshot(user)).willReturn(new HashMap<>());

            // when & then
            assertThatThrownBy(() -> userService.withdrawal(email))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("detailMessage")
                    .isEqualTo("이미 탈퇴처리된 회원 입니다.");

            assertThat(user.getUserStatus()).isEqualTo("탈퇴");

            verify(userRepository).findByEmail(email);
            verify(snapshotFunc).snapshot(user);
            verify(activityLogService, never()).log(any(), any(), any(), any(), any(), any(), any());
        }
    }

    @Nested
    class ProfileImagesTest {

        @Test
        @DisplayName("프로필 이미지 수정 성공")
        void setProfileImages_success() {
            // given
            Long userId = 1L;

            String introduction = "소개글 수정";

            MultipartFile file1 = org.mockito.Mockito.mock(MultipartFile.class);
            MultipartFile file2 = org.mockito.Mockito.mock(MultipartFile.class);
            List<MultipartFile> files = List.of(file1, file2);

            User userBefore = User.builder()
                    .id(userId)
                    .email("test001@sample.com")
                    .nickname("test001")
                    .userStatus("정상")
                    .build();

            User reloadedUser = User.builder()
                    .id(userId)
                    .email("test001@sample.com")
                    .nickname("test001")
                    .userStatus("정상")
                    .build();

            given(userRepository.findById(userId))
                    .willReturn(Optional.of(userBefore))
                    .willReturn(Optional.of(reloadedUser));

            given(snapshotFunc.snapshot(userBefore)).willReturn(new HashMap<>());
            given(snapshotFunc.snapshot(reloadedUser)).willReturn(new HashMap<>());

            // when
            UserResponse response = userService.setProfileImages(userId, introduction, files);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getMessage()).isEqualTo("프로파일 수정에 성공 하였습니다.");
            assertThat(response.getData()).isNull();

            verify(userRepository, times(2)).findById(userId);
            verify(profileImageService).uploadManyFiles(userId, files);
            verify(activityLogService).log(
                    eq(ActivityEntityType.USER),
                    eq(ActivityAction.UPDATE),
                    eq(reloadedUser.getId()),
                    eq(reloadedUser.logMessage()),
                    eq(reloadedUser),
                    eq(new HashMap<>()),
                    eq(new HashMap<>())
            );
            verify(diaryCacheRepository).evictDiaryCacheByUser(userId);
            verify(commentCacheRepository).evictCommentCacheByUser(userId);
        }

        @Test
        @DisplayName("프로필 이미지 수정 실패 - 사용자를 찾을 수 없음")
        void setProfileImages_fail_userNotFoundAtFirstLookup() {
            // given
            Long userId = 1L;
            List<MultipartFile> files = List.of();

            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.setProfileImages(userId, "", files))
                    .isInstanceOf(NotFoundException.class)
                    .extracting("detailMessage")
                    .isEqualTo("해당 사용자를 찾을 수 없습니다.");

            verify(profileImageService, never()).uploadManyFiles(any(), any());
            verify(activityLogService, never()).log(any(), any(), any(), any(), any(), any(), any());
            verify(diaryCacheRepository, never()).evictDiaryCacheByUser(any());
            verify(commentCacheRepository, never()).evictCommentCacheByUser(any());
        }

        @Test
        @DisplayName("프로필 이미지 수정 실패 - 업로드 후 재조회 시 사용자를 찾을 수 없음")
        void setProfileImages_fail_userNotFoundAtReload() {
            // given
            Long userId = 1L;
            String introduction = "소개글 수정";
            List<MultipartFile> files = List.of();

            User user = User.builder()
                    .id(userId)
                    .email("test001@sample.com")
                    .nickname("test001")
                    .userStatus("정상")
                    .build();

            given(userRepository.findById(userId))
                    .willReturn(Optional.of(user))
                    .willReturn(Optional.empty());

            given(snapshotFunc.snapshot(user)).willReturn(new HashMap<>());

            // when & then
            assertThatThrownBy(() -> userService.setProfileImages(userId, introduction, files))
                    .isInstanceOf(NotFoundException.class)
                    .extracting("detailMessage")
                    .isEqualTo("해당 사용자를 찾을 수 없습니다.");

            verify(profileImageService).uploadManyFiles(userId, files);
            verify(activityLogService, never()).log(any(), any(), any(), any(), any(), any(), any());
            verify(diaryCacheRepository, never()).evictDiaryCacheByUser(any());
            verify(commentCacheRepository, never()).evictCommentCacheByUser(any());
        }
    }

    @Nested
    class AdminLoginTest {

        @Test
        @DisplayName("관리자 로그인 성공")
        void admin_login_success() {
            LoginRequest request = LoginRequest.builder()
                    .email("test001@sample.com")
                    .password("12341234aaaa")
                    .build();

            Roles roleUser = Roles.builder()
                    .name("ROLE_ADMIN")
                    .build();

            User user = User.builder()
                    .id(1L)
                    .email("test001@sample.com")
                    .nickname("test001")
                    .password("encodedPassword")
                    .userStatus("정상")
                    .userRoles(List.of(
                            UserRoles.builder()
                                    .roles(roleUser)
                                    .build()
                    ))
                    .build();

            when(userRepository.findByEmail("test001@sample.com"))
                    .thenReturn(Optional.of(user));
            when(passwordEncoder.matches(request.getPassword(), user.getPassword()))
                    .thenReturn(true);

            when(jwtTokenProvider.createRefreshToken(user.getEmail()))
                    .thenReturn("new-refresh-token");

            when(userLoginHistRepository.save(any(UserLoginHist.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            when(refreshTokenRepository.save(any(RefreshToken.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            List<Object> objects = userService.adminLogin(request, httpServletRequest);

            assertThat(objects).isNotNull();
            assertThat(objects).isNotEmpty();
        }

        @Test
        @DisplayName("관리자 로그인 실패 - 빈 이메일")
        void admin_login_fail_emptyEmail() {
            LoginRequest request = LoginRequest.builder()
                    .email("")
                    .password("12341234aaaa")
                    .build();

            assertThatThrownBy(() -> userService.adminLogin(request, httpServletRequest))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("detailMessage")
                    .isEqualTo("이메일이나 비밀번호가 잘못 입력되었습니다.");
        }

        @Test
        @DisplayName("관리자 로그인 실패 - 잘못된 이메일 형식")
        void admin_login_fail_invalidEmail() {
            LoginRequest request = LoginRequest.builder()
                    .email("123456789")
                    .password("12341234aaaa")
                    .build();

            assertThatThrownBy(() -> userService.adminLogin(request, httpServletRequest))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("detailMessage")
                    .isEqualTo("이메일이나 비밀번호가 잘못 입력되었습니다.");
        }

        @Test
        @DisplayName("관리자 로그인 실패 - 비밀번호가 틀렸을 때")
        void admin_login_fail_mismatchPassword() {
            LoginRequest request = LoginRequest.builder()
                    .email("test001@sample.com")
                    .password("12341234aaaa")
                    .build();

            Roles roleUser = Roles.builder()
                    .name("ROLE_ADMIN")
                    .build();

            User user = User.builder()
                    .id(1L)
                    .email("test001@sample.com")
                    .nickname("test001")
                    .password("encodedPassword")
                    .userStatus("정상")
                    .userRoles(List.of(
                            UserRoles.builder()
                                    .roles(roleUser)
                                    .build()
                    ))
                    .build();

            when(userRepository.findByEmail("test001@sample.com"))
                    .thenReturn(Optional.of(user));

            when(passwordEncoder.matches(request.getPassword(), user.getPassword()))
                    .thenReturn(false);

            assertThatThrownBy(() -> userService.adminLogin(request, httpServletRequest))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("detailMessage")
                    .isEqualTo("이메일이나 비밀번호가 잘못 입력되었습니다.");
        }

        @Test
        @DisplayName("관리자 로그인 - 이미 탈퇴한 계정")
        void admin_login_fail_alreadyWithdrawal() {
            LoginRequest request = LoginRequest.builder()
                    .email("test001@sample.com")
                    .password("12341234aaaa")
                    .build();

            Roles roleUser = Roles.builder()
                    .name("ROLE_ADMIN")
                    .build();

            User user = User.builder()
                    .id(1L)
                    .email("test001@sample.com")
                    .nickname("test001")
                    .password("encodedPassword")
                    .userStatus("탈퇴")
                    .userRoles(List.of(
                            UserRoles.builder()
                                    .roles(roleUser)
                                    .build()
                    ))
                    .build();

            when(userRepository.findByEmail("test001@sample.com"))
                    .thenReturn(Optional.of(user));

            when(passwordEncoder.matches(request.getPassword(), user.getPassword()))
                    .thenReturn(true);

            assertThatThrownBy(() -> userService.adminLogin(request, httpServletRequest))
                    .isInstanceOf(AccessDeniedException.class)
                    .extracting("detailMessage")
                    .isEqualTo("탈퇴한 계정입니다.");
        }

        @Test
        @DisplayName("관리자 로그인 - 관리자 권한이 없을 때")
        void admin_login_fail_hasNoAuthority() {
            LoginRequest request = LoginRequest.builder()
                    .email("test001@sample.com")
                    .password("12341234aaaa")
                    .build();

            Roles roleUser = Roles.builder()
                    .name("ROLE_USER")
                    .build();

            User user = User.builder()
                    .id(1L)
                    .email("test001@sample.com")
                    .nickname("test001")
                    .password("encodedPassword")
                    .userStatus("정상")
                    .userRoles(List.of(
                            UserRoles.builder()
                                    .roles(roleUser)
                                    .build()
                    ))
                    .build();

            when(userRepository.findByEmail("test001@sample.com"))
                    .thenReturn(Optional.of(user));

            when(passwordEncoder.matches(request.getPassword(), user.getPassword()))
                    .thenReturn(true);

            assertThatThrownBy(() -> userService.adminLogin(request, httpServletRequest))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("detailMessage")
                    .isEqualTo("관리자 권한이 없습니다.");
        }
    }

    @Nested
    class getAllUsersInfoTest {
        @Test
        void getAllUsersInfo_정상동작() {
            // given
            Pageable pageable = PageRequest.of(0, 10);

            User user = User.builder()
                    .id(1L)
                    .email("test@test.com")
                    .nickname("tester")
                    .followerList(Arrays.asList(new Follow(), new Follow())) // 2명
                    .followingList(List.of(new Follow())) // 1명
                    .build();

            Page<User> userPage = new PageImpl<>(List.of(user));

            UserInfoResponse mockResponse = UserInfoResponse.builder()
                    .userId(1L)
                    .email("test@test.com")
                    .nickname("tester")
                    .build();

            when(userRepository.findAll(pageable)).thenReturn(userPage);
            when(userMapper.toResponse(user, 2L, 1L)).thenReturn(mockResponse);

            // when
            Page<UserInfoResponse> result = userService.getAllUsersInfo(pageable);

            // then
            UserInfoResponse response = result.getContent().get(0);

            assertEquals(1L, response.getUserId());
            assertEquals("test@test.com", response.getEmail());
            assertEquals("tester", response.getNickname());

            // 🔥 핵심 검증
            verify(userMapper).toResponse(user, 2L, 1L);
        }

        @Test
        void getAllUsersInfo_유저없음() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> emptyPage = new PageImpl<>(Collections.emptyList());

            when(userRepository.findAll(pageable)).thenReturn(emptyPage);

            // when
            Page<UserInfoResponse> result = userService.getAllUsersInfo(pageable);

            // then
            assertTrue(result.isEmpty());
            verify(userMapper, never()).toResponse(any(), anyLong(), anyLong());
        }
    }
}
