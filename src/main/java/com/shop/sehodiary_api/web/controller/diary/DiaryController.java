package com.shop.sehodiary_api.web.controller.diary;

import com.shop.sehodiary_api.config.restpage.RestPage;
import com.shop.sehodiary_api.repository.user.userDetails.CustomUserDetails;
import com.shop.sehodiary_api.service.diary.DiaryService;
import com.shop.sehodiary_api.web.dto.diary.DiaryRequest;
import com.shop.sehodiary_api.web.dto.diary.DiaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/diary")
@RequiredArgsConstructor
public class DiaryController {
    private final DiaryService diaryService;

    @GetMapping("/public")
    public ResponseEntity<RestPage<DiaryResponse>> getDiariesByPublic(
            @AuthenticationPrincipal CustomUserDetails customUserDetails, Pageable pageable
    ) {
        Long loginUserId = customUserDetails != null ? customUserDetails.getId() : null;
        return ResponseEntity.ok(new RestPage<>(diaryService.getDiariesByPublic(loginUserId, pageable)));
    }

    @GetMapping("/friends")
    public ResponseEntity<RestPage<DiaryResponse>> getDiariesByFriends(
            @AuthenticationPrincipal CustomUserDetails customUserDetails, Pageable pageable
    ) {
        Long loginUserId = customUserDetails != null ? customUserDetails.getId() : null;
        return ResponseEntity.ok(new RestPage<>(diaryService.getDiariesByFriends(loginUserId, pageable)));
    }

    @GetMapping("/user")
    public ResponseEntity<RestPage<DiaryResponse>> getDiariesByUser(
            @AuthenticationPrincipal CustomUserDetails customUserDetails, Pageable pageable
    ) {
        return ResponseEntity.ok(new RestPage<>(diaryService.getDiariesByUser(customUserDetails.getId(), customUserDetails.getId(), pageable)));
    }

    @GetMapping("/{targetUserId}/user")
    public ResponseEntity<RestPage<DiaryResponse>> getDiariesPublicAndFriendsByUser(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long targetUserId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(new RestPage<>(diaryService.getDiariesPublicAndFriendsByUser(customUserDetails.getId(), targetUserId, pageable)));
    }

    @GetMapping("/one/{diaryId}")
    public ResponseEntity<DiaryResponse> getOneDiary(@PathVariable Long diaryId) {
        return ResponseEntity.ok(diaryService.getOneDiary(diaryId));
    }

    @PostMapping(path = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DiaryResponse> createDiary(@AuthenticationPrincipal CustomUserDetails customUserDetails, @RequestPart DiaryRequest request, @RequestPart(required = false) List<MultipartFile> files) {
        return ResponseEntity.ok(diaryService.createDiary(customUserDetails.getId(), request, files));
    }

    @PutMapping(path = "/edit/{diaryId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DiaryResponse> editDiary(@AuthenticationPrincipal CustomUserDetails customUserDetails, @PathVariable Long diaryId, @RequestPart DiaryRequest request, @RequestPart(required = false) List<MultipartFile> files) {
        return ResponseEntity.ok(diaryService.editDiary(customUserDetails.getId(), diaryId, request, files));
    }

    @DeleteMapping("/{diaryId}")
    public ResponseEntity<Void> deleteDiary(@AuthenticationPrincipal CustomUserDetails customUserDetails, @PathVariable Long diaryId) {
        diaryService.deleteDiary(customUserDetails.getId(), diaryId);
        return ResponseEntity.ok().build();
    }
}
