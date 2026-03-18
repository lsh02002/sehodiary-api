package com.shop.sehodiary_api.service.user;

import com.shop.sehodiary_api.repository.activity.function.SnapshotFunc;
import com.shop.sehodiary_api.repository.user.User;
import com.shop.sehodiary_api.repository.user.UserRepository;
import com.shop.sehodiary_api.repository.user.userRoles.Roles;
import com.shop.sehodiary_api.repository.user.userRoles.RolesRepository;
import com.shop.sehodiary_api.repository.user.userRoles.UserRoles;
import com.shop.sehodiary_api.repository.user.userRoles.UserRolesRepository;
import com.shop.sehodiary_api.service.activelog.ActivityLogService;
import com.shop.sehodiary_api.service.exceptions.BadRequestException;
import com.shop.sehodiary_api.service.exceptions.ConflictException;
import com.shop.sehodiary_api.web.dto.user.SignupRequest;
import com.shop.sehodiary_api.web.dto.user.SignupResponse;
import com.shop.sehodiary_api.web.dto.user.UserResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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

        assertThrows(ConflictException.class, () -> userService.signUp(request));
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

        assertThrows(ConflictException.class, () -> userService.signUp(request));
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

        assertThrows(BadRequestException.class, () -> userService.signUp(request));
    }
}
