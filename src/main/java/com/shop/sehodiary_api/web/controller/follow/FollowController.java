package com.shop.sehodiary_api.web.controller.follow;

import com.shop.sehodiary_api.repository.user.userDetails.CustomUserDetails;
import com.shop.sehodiary_api.service.follow.FollowService;
import com.shop.sehodiary_api.web.dto.follow.FollowRelationshipResponse;
import com.shop.sehodiary_api.web.dto.follow.FollowUserResponse;
import com.shop.sehodiary_api.web.dto.user.UserInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/follow")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping("/{targetUserId}/follow")
    public ResponseEntity<Void> follow(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                       @PathVariable Long targetUserId) {
        followService.follow(customUserDetails.getId(), targetUserId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{targetUserId}/follow")
    public ResponseEntity<Void> unfollow(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                         @PathVariable Long targetUserId) {
        followService.unfollow(customUserDetails.getId(), targetUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{targetUserId}/relationship")
    public ResponseEntity<FollowRelationshipResponse> relationship(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long targetUserId) {
        return ResponseEntity.ok(followService.getRelationship(customUserDetails.getId(), targetUserId));
    }

    @GetMapping("/following")
    public ResponseEntity<List<FollowUserResponse>> getFollowingList(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        return ResponseEntity.ok(followService.getFollowingList(customUserDetails.getId()));
    }

    @GetMapping("/follower")
    public ResponseEntity<List<FollowUserResponse>> getFollowerList(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        return ResponseEntity.ok(followService.getFollowerList(customUserDetails.getId()));
    }

    @GetMapping("/discover")
    public ResponseEntity<List<UserInfoResponse>> getDiscoverUsers(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        return ResponseEntity.ok(followService.getDiscoverUsers(customUserDetails.getId()));
    }
}
