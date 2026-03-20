package com.shop.sehodiary_api.web.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.sehodiary_api.repository.user.userDetails.CustomUserDetails;
import com.shop.sehodiary_api.service.exceptions.AccessDeniedException;
import com.shop.sehodiary_api.service.exceptions.NotAcceptableException;
import com.shop.sehodiary_api.service.user.UserService;
import com.shop.sehodiary_api.web.dto.user.LoginRequest;
import com.shop.sehodiary_api.web.dto.user.SignupRequest;
import com.shop.sehodiary_api.web.dto.user.UserInfoResponse;
import com.shop.sehodiary_api.web.dto.user.UserResponse;
import com.shop.sehodiary_api.web.dto.user.userLoginHist.UserLoginHistResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    private static final Long USER_ID = 1L;
    private static final String USER_EMAIL = "test@test.com";

    private CustomUserDetails createCustomUserDetails(Long userId, String email, String role) {
        CustomUserDetails userDetails = Mockito.mock(CustomUserDetails.class);
        given(userDetails.getId()).willReturn(userId);
        given(userDetails.getEmail()).willReturn(email);
        return userDetails;
    }

    private UsernamePasswordAuthenticationToken createAuthentication() {
        CustomUserDetails userDetails = createCustomUserDetails(USER_ID, USER_EMAIL, "ROLE_USER");
        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    private UsernamePasswordAuthenticationToken createAdminAuthentication() {
        CustomUserDetails userDetails = createCustomUserDetails(USER_ID, USER_EMAIL, "ROLE_ADMIN");
        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    @Nested
    @DisplayName("회원가입")
    @WithMockUser(roles = "USER")
    class SignUpTest {

        @Test
        @DisplayName("POST /user/sign-up 성공")
        void signUp_success() throws Exception {
            SignupRequest request = Mockito.mock(SignupRequest.class);
            UserResponse response = Mockito.mock(UserResponse.class);

            given(userService.signUp(any(SignupRequest.class))).willReturn(response);

            mockMvc.perform(post("/user/sign-up")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)).with(csrf()))
                    .andExpect(status().isOk());

            then(userService).should().signUp(any(SignupRequest.class));
        }
    }

    @Nested
    @DisplayName("로그인")
    @WithMockUser(roles = "USER")
    class LoginTest {

        @Test
        @DisplayName("POST /user/login 성공")
        void login_success() throws Exception {
            LoginRequest request = Mockito.mock(LoginRequest.class);
            UserResponse response = Mockito.mock(UserResponse.class);

            List<Object> serviceResult = List.of(
                    "access-token-value",
                    "refresh-token-value",
                    response
            );

            given(userService.login(any(LoginRequest.class), any())).willReturn(serviceResult);

            mockMvc.perform(post("/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(header().string("accessToken", "access-token-value"))
                    .andExpect(header().string("refreshToken", "refresh-token-value"));

            then(userService).should().login(any(LoginRequest.class), any());
        }

        @Test
        @DisplayName("POST /user/admin-login 성공")
        void adminLogin_success() throws Exception {
            LoginRequest request = Mockito.mock(LoginRequest.class);
            UserResponse response = Mockito.mock(UserResponse.class);

            List<Object> serviceResult = List.of(
                    "admin-access-token",
                    "admin-refresh-token",
                    response
            );

            given(userService.adminLogin(any(LoginRequest.class), any())).willReturn(serviceResult);

            mockMvc.perform(post("/user/admin-login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(header().string("accessToken", "admin-access-token"))
                    .andExpect(header().string("refreshToken", "admin-refresh-token"));

            then(userService).should().adminLogin(any(LoginRequest.class), any());
        }
    }

    @Nested
    @DisplayName("로그아웃 / 탈퇴")
    class LogoutAndWithdrawalTest {

        @Test
        @DisplayName("DELETE /user/logout 성공")
        void logout_success() throws Exception {
            UserResponse response = Mockito.mock(UserResponse.class);

            given(userService.logout(eq(USER_EMAIL), any(), any())).willReturn(response);

            mockMvc.perform(delete("/user/logout")
                            .with(authentication(createAuthentication())).with(csrf()))
                    .andExpect(status().isOk());

            then(userService).should().logout(eq(USER_EMAIL), any(), any());
        }

        @Test
        @DisplayName("DELETE /user/withdrawal 성공")
        void withdrawal_success() throws Exception {
            UserResponse response = Mockito.mock(UserResponse.class);

            given(userService.withdrawal(USER_EMAIL)).willReturn(response);

            mockMvc.perform(delete("/user/withdrawal")
                            .with(authentication(createAuthentication())).with(csrf()))
                    .andExpect(status().isOk());

            then(userService).should().withdrawal(USER_EMAIL);
        }
    }

    @Nested
    @DisplayName("프로필")
    class ProfileTest {

        @Test
        @DisplayName("POST /user/profile 성공 - 파일 업로드")
        void setProfileImages_success() throws Exception {
            UserResponse response = Mockito.mock(UserResponse.class);

            MockMultipartFile file1 = new MockMultipartFile(
                    "files",
                    "profile1.jpg",
                    MediaType.IMAGE_JPEG_VALUE,
                    "image-content-1".getBytes()
            );

            MockMultipartFile file2 = new MockMultipartFile(
                    "files",
                    "profile2.jpg",
                    MediaType.IMAGE_JPEG_VALUE,
                    "image-content-2".getBytes()
            );

            given(userService.setProfileImages(eq(USER_ID), any())).willReturn(response);

            mockMvc.perform(multipart("/user/profile")
                            .file(file1)
                            .file(file2)
                            .with(authentication(createAuthentication()))
                            .contentType(MediaType.MULTIPART_FORM_DATA).with(csrf()))
                    .andExpect(status().isOk());

            then(userService).should().setProfileImages(eq(USER_ID), any());
        }

        @Test
        @DisplayName("POST /user/profile 성공 - 파일 없이 요청")
        void setProfileImages_withoutFiles_success() throws Exception {
            UserResponse response = Mockito.mock(UserResponse.class);

            given(userService.setProfileImages(eq(USER_ID), any())).willReturn(response);

            mockMvc.perform(multipart("/user/profile")
                            .with(authentication(createAuthentication()))
                            .contentType(MediaType.MULTIPART_FORM_DATA).with(csrf()))
                    .andExpect(status().isOk());

            then(userService).should().setProfileImages(eq(USER_ID), any());
        }
    }

    @Nested
    @DisplayName("유저 정보 조회")
    class UserInfoTest {

        @Test
        @DisplayName("GET /user/info 성공")
        void getUserInfo_success() throws Exception {
            UserInfoResponse response = Mockito.mock(UserInfoResponse.class);

            given(userService.getUserInfo(any(CustomUserDetails.class))).willReturn(response);

            mockMvc.perform(get("/user/info")
                            .with(authentication(createAuthentication())))
                    .andExpect(status().isOk());

            then(userService).should().getUserInfo(any(CustomUserDetails.class));
        }
    }

    @Nested
    @DisplayName("로그인 이력")
    class LoginHistTest {

        @Test
        @DisplayName("GET /user/hist 성공")
        void getUserLoginHist_success() throws Exception {
            UserLoginHistResponse hist1 = Mockito.mock(UserLoginHistResponse.class);
            UserLoginHistResponse hist2 = Mockito.mock(UserLoginHistResponse.class);
            Page<UserLoginHistResponse> page = new PageImpl<>(List.of(hist1, hist2), PageRequest.of(0, 20), 2);

            given(userService.getUserLoginHist(eq(USER_ID), any())).willReturn(page);

            mockMvc.perform(get("/user/hist")
                            .param("page", "0")
                            .param("size", "20")
                            .with(authentication(createAuthentication())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2));

            then(userService).should().getUserLoginHist(eq(USER_ID), any());
        }

        @Test
        @DisplayName("GET /user/hist/{userId} 성공 - 관리자")
        void getUserLoginHistByAdmin_success() throws Exception {
            Long targetUserId = 99L;
            UserLoginHistResponse hist1 = Mockito.mock(UserLoginHistResponse.class);
            Page<UserLoginHistResponse> page = new PageImpl<>(List.of(hist1), PageRequest.of(0, 10), 1);

            given(userService.getUserLoginHist(eq(targetUserId), any())).willReturn(page);

            mockMvc.perform(get("/user/hist/{userId}", targetUserId)
                            .param("page", "0")
                            .param("size", "10")
                            .with(authentication(createAdminAuthentication())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1));

            then(userService).should().getUserLoginHist(eq(targetUserId), any());
        }
    }

    @Nested
    @DisplayName("예외 엔드포인트")
    @WithMockUser(roles = "USER")
    class ExceptionEndpointTest {

        @Test
        @DisplayName("GET /user/entrypoint - accessToken 없으면 NotAcceptableException")
        void entrypointException_withoutToken() throws Exception {
            mockMvc.perform(get("/user/entrypoint"))
                    .andExpect(result ->
                            assertInstanceOf(NotAcceptableException.class, result.getResolvedException()));
        }

        @Test
        @DisplayName("GET /user/entrypoint - accessToken 있으면 만료 예외")
        void entrypointException_withToken() throws Exception {
            mockMvc.perform(get("/user/entrypoint")
                            .param("accessToken", "expired-token"))
                    .andExpect(result ->
                            assertInstanceOf(NotAcceptableException.class, result.getResolvedException()));
        }

        @Test
        @DisplayName("GET /user/access-denied - roles 없으면 AccessDeniedException")
        void accessDeniedException_withoutRoles() throws Exception {
            mockMvc.perform(get("/user/access-denied"))
                    .andExpect(result ->
                            assertInstanceOf(AccessDeniedException.class, result.getResolvedException()));
        }

        @Test
        @DisplayName("GET /user/access-denied - roles 있으면 권한 없음 예외")
        void accessDeniedException_withRoles() throws Exception {
            mockMvc.perform(get("/user/access-denied")
                            .param("roles", "ROLE_USER"))
                    .andExpect(result ->
                            assertInstanceOf(AccessDeniedException.class, result.getResolvedException()));
        }
    }

    @Nested
    @DisplayName("테스트 엔드포인트")
    @WithMockUser(roles = "USER")
    class TestEndpointTest {

        @Test
        @DisplayName("GET /user/test1 성공")
        void test1_success() throws Exception {
            CustomUserDetails userDetails = createCustomUserDetails(USER_ID, USER_EMAIL, "ROLE_USER");
            given(userDetails.toString()).willReturn("custom-user-details");

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_USER"))
                    );

            mockMvc.perform(get("/user/test1")
                            .with(authentication(authentication)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("custom-user-details"));
        }

        @Test
        @DisplayName("GET /user/test2 성공")
        void test2_success() throws Exception {
            mockMvc.perform(get("/user/test2"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Jwt 토큰이 상관없는 EntryPoint 테스트입니다."));
        }
    }

    @Nested
    @DisplayName("관리자 전용")
    class AdminTest {

        @Test
        @DisplayName("GET /user/all-users-info 성공 - 관리자")
        void getAllUsersInfo_success() throws Exception {
            UserInfoResponse user1 = Mockito.mock(UserInfoResponse.class);
            UserInfoResponse user2 = Mockito.mock(UserInfoResponse.class);
            Page<UserInfoResponse> page = new PageImpl<>(List.of(user1, user2), PageRequest.of(0, 20), 2);

            given(userService.getAllUsersInfo(any())).willReturn(page);

            mockMvc.perform(get("/user/all-users-info")
                            .param("page", "0")
                            .param("size", "20")
                            .with(authentication(createAdminAuthentication())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2));

            then(userService).should().getAllUsersInfo(any());
        }
    }
}