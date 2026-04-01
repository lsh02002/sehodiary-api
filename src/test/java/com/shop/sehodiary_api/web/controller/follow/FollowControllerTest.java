package com.shop.sehodiary_api.web.controller.follow;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;
import java.util.List;

import com.shop.sehodiary_api.repository.user.userDetails.CustomUserDetails;
import com.shop.sehodiary_api.service.follow.FollowService;
import com.shop.sehodiary_api.web.dto.follow.FollowRelationshipResponse;
import com.shop.sehodiary_api.web.dto.follow.FollowUserResponse;
import com.shop.sehodiary_api.web.dto.user.UserInfoResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FollowController.class)
class FollowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FollowService followService;

    private UsernamePasswordAuthenticationToken createAuth(Long userId) {
        CustomUserDetails principal = CustomUserDetails.builder()
                .id(userId)
                .email("test@example.com")
                .password("password")
                .authorities(Collections.emptyList())
                .build();

        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
    }

    @Nested
    @DisplayName("POST /follow/{targetUserId}/follow")
    class FollowTest {

        @Test
        @DisplayName("팔로우 요청이 성공하면 200 OK를 반환한다")
        void follow_success() throws Exception {
            // given
            Long loginUserId = 1L;
            Long targetUserId = 2L;

            doNothing().when(followService).follow(loginUserId, targetUserId);

            // when & then
            mockMvc.perform(post("/follow/{targetUserId}/follow", targetUserId)
                            .with(authentication(createAuth(loginUserId)))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            then(followService).should().follow(eq(loginUserId), eq(targetUserId));
        }
    }

    @Nested
    @DisplayName("DELETE /follow/{targetUserId}/follow")
    class UnfollowTest {

        @Test
        @DisplayName("언팔로우 요청이 성공하면 204 No Content를 반환한다")
        void unfollow_success() throws Exception {
            // given
            Long loginUserId = 1L;
            Long targetUserId = 2L;

            doNothing().when(followService).unfollow(loginUserId, targetUserId);

            // when & then
            mockMvc.perform(delete("/follow/{targetUserId}/follow", targetUserId)
                            .with(authentication(createAuth(loginUserId)))
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            then(followService).should().unfollow(eq(loginUserId), eq(targetUserId));
        }
    }

    @Nested
    @DisplayName("GET /follow/{targetUserId}/relationship")
    class RelationshipTest {

        @Test
        @DisplayName("팔로우 관계 조회에 성공하면 200 OK와 관계 정보를 반환한다")
        void relationship_success() throws Exception {
            // given
            Long loginUserId = 1L;
            Long targetUserId = 2L;

            FollowRelationshipResponse response =
                    new FollowRelationshipResponse(true, false, false);

            given(followService.getRelationship(loginUserId, targetUserId))
                    .willReturn(response);

            // when & then
            mockMvc.perform(get("/follow/{targetUserId}/relationship", targetUserId)
                            .with(authentication(createAuth(loginUserId))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isFollowing").value(true))
                    .andExpect(jsonPath("$.isFollowedBy").value(false))
                    .andExpect(jsonPath("$.isMutual").value(false));

            then(followService).should().getRelationship(eq(loginUserId), eq(targetUserId));
        }
    }

    @Nested
    @DisplayName("GET /follow/following")
    class GetFollowingListTest {

        @Test
        @DisplayName("내가 팔로우하는 목록을 조회한다")
        void getFollowingList_success() throws Exception {
            // given
            Long loginUserId = 1L;

            List<FollowUserResponse> response = List.of(
                    FollowUserResponse.builder()
                            .userId(2L)
                            .nickname("user2")
                            .profileImageUrl("https://cdn.com/user2.png")
                            .build(),
                    FollowUserResponse.builder()
                            .userId(3L)
                            .nickname("user3")
                            .profileImageUrl("https://cdn.com/user3.png")
                            .build()
            );

            given(followService.getFollowingList(loginUserId)).willReturn(response);

            // when & then
            mockMvc.perform(get("/follow/following")
                            .with(authentication(createAuth(loginUserId))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].userId").value(2L))
                    .andExpect(jsonPath("$[0].nickname").value("user2"))
                    .andExpect(jsonPath("$[0].profileImageUrl").value("https://cdn.com/user2.png"))
                    .andExpect(jsonPath("$[1].userId").value(3L))
                    .andExpect(jsonPath("$[1].nickname").value("user3"))
                    .andExpect(jsonPath("$[1].profileImageUrl").value("https://cdn.com/user3.png"));

            then(followService).should().getFollowingList(loginUserId);
        }

        @Test
        @DisplayName("내가 팔로우하는 목록이 없으면 빈 배열을 반환한다")
        void getFollowingList_empty() throws Exception {
            // given
            Long loginUserId = 1L;
            given(followService.getFollowingList(loginUserId)).willReturn(List.of());

            // when & then
            mockMvc.perform(get("/follow/following")
                            .with(authentication(createAuth(loginUserId))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));

            then(followService).should().getFollowingList(loginUserId);
        }
    }

    @Nested
    @DisplayName("GET /follow/follower")
    class GetFollowerListTest {

        @Test
        @DisplayName("나를 팔로우하는 목록을 조회한다")
        void getFollowerList_success() throws Exception {
            // given
            Long loginUserId = 1L;

            List<FollowUserResponse> response = List.of(
                    FollowUserResponse.builder()
                            .userId(10L)
                            .nickname("follower1")
                            .profileImageUrl("https://cdn.com/follower1.png")
                            .build(),
                    FollowUserResponse.builder()
                            .userId(11L)
                            .nickname("follower2")
                            .profileImageUrl("https://cdn.com/follower2.png")
                            .build()
            );

            given(followService.getFollowerList(loginUserId)).willReturn(response);

            // when & then
            mockMvc.perform(get("/follow/follower")
                            .with(authentication(createAuth(loginUserId))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].userId").value(10L))
                    .andExpect(jsonPath("$[0].nickname").value("follower1"))
                    .andExpect(jsonPath("$[0].profileImageUrl").value("https://cdn.com/follower1.png"))
                    .andExpect(jsonPath("$[1].userId").value(11L))
                    .andExpect(jsonPath("$[1].nickname").value("follower2"))
                    .andExpect(jsonPath("$[1].profileImageUrl").value("https://cdn.com/follower2.png"));

            then(followService).should().getFollowerList(loginUserId);
        }

        @Test
        @DisplayName("나를 팔로우하는 목록이 없으면 빈 배열을 반환한다")
        void getFollowerList_empty() throws Exception {
            // given
            Long loginUserId = 1L;
            given(followService.getFollowerList(loginUserId)).willReturn(List.of());

            // when & then
            mockMvc.perform(get("/follow/follower")
                            .with(authentication(createAuth(loginUserId))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));

            then(followService).should().getFollowerList(loginUserId);
        }
    }

    @Nested
    class GetDiscoverUsersTest {
        @Test
        @WithMockUser(authorities = "ROLE_USER")
        void getDiscoverUsers_성공() throws Exception {
            // given
            Long userId = 1L;

            UserInfoResponse user1 = new UserInfoResponse(2L, "user1@mail.com", "user1", null, null, 0L, 0L);
            UserInfoResponse user2 = new UserInfoResponse(3L, "user2@mail.com", "user2", null, null, 0L, 0L);

            given(followService.getDiscoverUsers(userId))
                    .willReturn(List.of(user1, user2));

            // CustomUserDetails mock 주입
            CustomUserDetails customUserDetails = new CustomUserDetails(userId, "test@mail.com", "password", "test", List.of("ROLE_USER"));

            // when & then
            mockMvc.perform(get("/follow/discover")
                            .with(authentication(new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities()))))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").value(2L))
                    .andExpect(jsonPath("$[0].nickname").value("user1"))
                    .andExpect(jsonPath("$[1].id").value(3L))
                    .andExpect(jsonPath("$[1].nickname").value("user2"));
        }
    }
}