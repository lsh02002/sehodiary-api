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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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
    private SnapshotFunc snapshotFunc;

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private HttpServletResponse httpServletResponse;

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
                    .build();

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(s3Address.siteAddress()).willReturn("https://cdn.sample.com");

            // when
            UserInfoResponse response = userService.getUserInfo(customUserDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getEmail()).isEqualTo("test001@sample.com");
            assertThat(response.getNickname()).isEqualTo("test001");
            assertThat(response.getProfileImages()).hasSize(2);
            assertThat(response.getProfileImages()).containsExactly(
                    "https://cdn.sample.com/profile/user1.png",
                    "https://cdn.sample.com/profile/user2.png"
            );

            verify(userRepository).findById(userId);
        }

        @Test
        @DisplayName("내 정보 조회 성공 - 프로필 이미지가 없으면 빈 리스트를 반환한다")
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
                    .build();

            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // when
            UserInfoResponse response = userService.getUserInfo(customUserDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getProfileImages()).isEmpty();
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
    class LogoutTest {

        @Test
        @DisplayName("로그아웃 성공 - accessToken이 유효하면 refreshToken 삭제 후 블랙리스트 등록")
        void logout_success_validAccessToken() {
            // given
            String email = "test001@sample.com";
            String refreshToken = "refresh-token";
            String accessToken = "access-token";

            given(httpServletRequest.getHeader("refreshToken")).willReturn(refreshToken);
            given(httpServletRequest.getHeader("accessToken")).willReturn(accessToken);
            given(jwtTokenProvider.validateAccessToken(accessToken)).willReturn(true);

            // when
            UserResponse result = userService.logout(email, httpServletRequest, httpServletResponse);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getMessage()).isEqualTo("로그아웃에 성공 하였습니다.");
            assertThat(result.getData()).isNull();

            verify(refreshTokenRepository).deleteByRefreshToken(refreshToken);
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

            given(httpServletRequest.getHeader("refreshToken")).willReturn(refreshToken);
            given(httpServletRequest.getHeader("accessToken")).willReturn(accessToken);
            given(jwtTokenProvider.validateAccessToken(accessToken)).willReturn(false);

            // when
            UserResponse result = userService.logout(email, httpServletRequest, httpServletResponse);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getMessage()).isEqualTo("로그아웃에 성공 하였습니다.");
            assertThat(result.getData()).isNull();

            verify(refreshTokenRepository).deleteByRefreshToken(refreshToken);
            verify(jwtTokenProvider).validateAccessToken(accessToken);
            verify(redisUtil, never()).setBlackList(anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt());
        }

        @Test
        @DisplayName("로그아웃 실패 - email이 null이면 BadRequestException 발생")
        void logout_fail_emailIsNull() {
            // given
            String email = null;

            // when & then
            assertThatThrownBy(() -> userService.logout(email, httpServletRequest, httpServletResponse))
                    .isInstanceOf(BadRequestException.class)
                    .extracting("detailMessage")
                    .isEqualTo("유저 정보가 비어있습니다.");

            verify(refreshTokenRepository, never()).deleteByRefreshToken(anyString());
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
            UserResponse response = userService.setProfileImages(userId, files);

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
            assertThatThrownBy(() -> userService.setProfileImages(userId, files))
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
            assertThatThrownBy(() -> userService.setProfileImages(userId, files))
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
}
