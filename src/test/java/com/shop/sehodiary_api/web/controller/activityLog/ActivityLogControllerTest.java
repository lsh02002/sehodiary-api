package com.shop.sehodiary_api.web.controller.activityLog;

import com.shop.sehodiary_api.repository.user.userDetails.CustomUserDetails;
import com.shop.sehodiary_api.service.activelog.ActivityLogService;
import com.shop.sehodiary_api.web.dto.activitylog.ActivityLogResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ActivityLogController.class)
@AutoConfigureMockMvc
class ActivityLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActivityLogService activityLogService;

    private static final Long USER_ID = 1L;

    private CustomUserDetails createUserDetails() {
        CustomUserDetails userDetails = Mockito.mock(CustomUserDetails.class);
        given(userDetails.getId()).willReturn(USER_ID);
        return userDetails;
    }

    private UsernamePasswordAuthenticationToken createAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                createUserDetails(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Nested
    @DisplayName("GET /activitylog/user")
    class GetActivityLogsByUserTest {

        @Test
        @DisplayName("사용자 활동 로그 조회 성공")
        void getActivityLogs_success() throws Exception {
            ActivityLogResponse log1 = Mockito.mock(ActivityLogResponse.class);
            ActivityLogResponse log2 = Mockito.mock(ActivityLogResponse.class);

            given(activityLogService.getActivityLogsByUser(USER_ID))
                    .willReturn(List.of(log1, log2));

            mockMvc.perform(get("/activitylog/user")
                            .with(authentication(createAuthentication())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));

            then(activityLogService).should().getActivityLogsByUser(USER_ID);
        }

        @Test
        @DisplayName("활동 로그 없음 - 빈 리스트 반환")
        void getActivityLogs_empty() throws Exception {
            given(activityLogService.getActivityLogsByUser(USER_ID))
                    .willReturn(List.of());

            mockMvc.perform(get("/activitylog/user")
                            .with(authentication(createAuthentication())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));

            then(activityLogService).should().getActivityLogsByUser(USER_ID);
        }
    }
}