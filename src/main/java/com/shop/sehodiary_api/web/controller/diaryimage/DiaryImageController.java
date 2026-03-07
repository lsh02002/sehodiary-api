package com.shop.sehodiary_api.web.controller.diaryimage;

import com.shop.sehodiary_api.repository.user.userDetails.CustomUserDetails;
import com.shop.sehodiary_api.service.diaryimage.DiaryImageService;
import com.shop.sehodiary_api.web.dto.diaryimage.DiaryImageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/diaryimage")
@RequiredArgsConstructor
public class DiaryImageController {
    private final DiaryImageService diaryImageService;

    @PostMapping(path = "/{diaryId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DiaryImageResponse> uploadFile(@AuthenticationPrincipal CustomUserDetails customUserDetails, @PathVariable Long diaryId, @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(diaryImageService.uploadFileByUserIdAndDiaryId(customUserDetails.getId(), diaryId, file));
    }

    @PostMapping(path = "/{diaryId}/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<DiaryImageResponse>> uploadManyFiles(@AuthenticationPrincipal CustomUserDetails customUserDetails, @PathVariable Long diaryId, @RequestPart("files") List<MultipartFile> files) {
        return ResponseEntity.ok(diaryImageService.uploadManyFiles(customUserDetails.getId(), diaryId, files));
    }

    @GetMapping("/{diaryImageId}")
    public ResponseEntity<DiaryImageResponse> findDiaryImageById(@PathVariable Long diaryImageId) {
        return ResponseEntity.ok(diaryImageService.findDiaryImageId(diaryImageId));
    }

    @DeleteMapping("/{diaryImageId}")
    public ResponseEntity<Void> deleteDiaryImageById(@AuthenticationPrincipal CustomUserDetails customUserDetails, @PathVariable Long diaryImageId) {
        diaryImageService.deleteFileByUserIdAndDiaryImageId(customUserDetails.getId(), diaryImageId);
        return ResponseEntity.ok().build();
    }
}
