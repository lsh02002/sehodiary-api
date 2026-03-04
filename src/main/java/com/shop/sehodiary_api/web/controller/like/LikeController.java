package com.shop.sehodiary_api.web.controller.like;

import com.shop.sehodiary_api.repository.user.userDetails.CustomUserDetails;
import com.shop.sehodiary_api.service.like.LikeService;
import com.shop.sehodiary_api.web.dto.diary.DiaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/like")
@RequiredArgsConstructor
public class LikeController {
    private final LikeService likeService;

    @GetMapping("/nicknames/{diaryId}")
    public ResponseEntity<List<String>> getLikingNicknamesByDiary(@PathVariable Long diaryId) {
        return ResponseEntity.ok(likeService.getLikingNicknamesByDiary(diaryId));
    }

    @GetMapping("/isLiked/{id}")
    public ResponseEntity<Boolean> isLiked(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable("id") Long diaryId) {
        return ResponseEntity.ok(likeService.isLiked(userDetails.getId(), diaryId));
    }

    @GetMapping("/user")
    public ResponseEntity<List<DiaryResponse>> getMyLikedDiaries(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        return ResponseEntity.ok(likeService.getMyLikedDiaries(customUserDetails.getId()));
    }

    @PostMapping("/{id}")
    public ResponseEntity<Boolean> insert(@AuthenticationPrincipal CustomUserDetails customUserDetails, @PathVariable("id") Long diaryId) {
        return ResponseEntity.ok(likeService.insert(customUserDetails.getId(), diaryId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Boolean> delete(@AuthenticationPrincipal CustomUserDetails customUserDetails, @PathVariable("id") Long diaryId) {
        return ResponseEntity.ok(likeService.delete(customUserDetails.getId(), diaryId));
    }
}
