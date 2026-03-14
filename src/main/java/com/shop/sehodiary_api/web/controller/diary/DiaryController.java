package com.shop.sehodiary_api.web.controller.diary;

import com.shop.sehodiary_api.repository.user.userDetails.CustomUserDetails;
import com.shop.sehodiary_api.service.diary.DiaryService;
import com.shop.sehodiary_api.web.dto.diary.DiaryRequest;
import com.shop.sehodiary_api.web.dto.diary.DiaryResponse;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<List<DiaryResponse>> getDiariesByPublic() {
        return ResponseEntity.ok(diaryService.getDiariesByPublic());
    }

    @GetMapping("/friends")
    public ResponseEntity<List<DiaryResponse>> getDiariesByFriends() {
        return ResponseEntity.ok(diaryService.getDiariesByFriends());
    }

    @GetMapping("/user")
    public ResponseEntity<List<DiaryResponse>> getDiariesByUser(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        return ResponseEntity.ok(diaryService.getDiariesByUser(customUserDetails.getId()));
    }

    @GetMapping("/{diaryId}")
    public ResponseEntity<DiaryResponse> getOneDiary(@PathVariable Long diaryId) {
        return ResponseEntity.ok(diaryService.getOneDiary(diaryId));
    }

    @PostMapping(path = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DiaryResponse> createDiary(@AuthenticationPrincipal CustomUserDetails customUserDetails, @RequestPart DiaryRequest request, @RequestPart(required = false) List<MultipartFile> files) {
        return ResponseEntity.ok(diaryService.createDiary(customUserDetails.getId(), request, files));
    }

    @PostMapping(path = "/edit/{diaryId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DiaryResponse> editDiary(@AuthenticationPrincipal CustomUserDetails customUserDetails, @PathVariable Long diaryId, @RequestPart DiaryRequest request, @RequestPart(required = false) List<MultipartFile> files) {
        return ResponseEntity.ok(diaryService.editDiary(customUserDetails.getId(), diaryId, request, files));
    }

    @DeleteMapping("/{diaryId}")
    public ResponseEntity<Void> deleteDiary(@AuthenticationPrincipal CustomUserDetails customUserDetails, @PathVariable Long diaryId) {
        diaryService.deleteDiary(customUserDetails.getId(), diaryId);
        return ResponseEntity.ok().build();
    }
}
