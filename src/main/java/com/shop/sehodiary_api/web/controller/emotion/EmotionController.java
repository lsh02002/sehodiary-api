package com.shop.sehodiary_api.web.controller.emotion;

import com.shop.sehodiary_api.repository.user.userDetails.CustomUserDetails;
import com.shop.sehodiary_api.service.diary.DiaryService;
import com.shop.sehodiary_api.service.emotion.EmotionService;
import com.shop.sehodiary_api.web.dto.diary.DiaryRequest;
import com.shop.sehodiary_api.web.dto.diary.DiaryResponse;
import com.shop.sehodiary_api.web.dto.emotion.EmotionRequest;
import com.shop.sehodiary_api.web.dto.emotion.EmotionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/emotion")
@RequiredArgsConstructor
public class EmotionController {
    private final EmotionService emotionService;

    @GetMapping("/all")
    public ResponseEntity<List<EmotionResponse>> getAllEmotions() {
        return ResponseEntity.ok(emotionService.getAllEmotions());
    }

    @PostMapping("/create")
    public ResponseEntity<EmotionResponse> createEmotion(@AuthenticationPrincipal CustomUserDetails customUserDetails, @RequestBody EmotionRequest request) {
        return ResponseEntity.ok(emotionService.createEmotion(customUserDetails.getId(), request));
    }

    @PostMapping("/edit/{emotionId}")
    public ResponseEntity<EmotionResponse> editEmotion(@AuthenticationPrincipal CustomUserDetails customUserDetails, @PathVariable Long emotionId, @RequestBody EmotionRequest request) {
        return ResponseEntity.ok(emotionService.editEmotion(customUserDetails.getId(), emotionId, request));
    }

    @DeleteMapping("/{emotionId}")
    public ResponseEntity<Void> deleteEmotion(@AuthenticationPrincipal CustomUserDetails customUserDetails, @PathVariable Long emotionId) {
        emotionService.deleteEmotion(customUserDetails.getId(), emotionId);
        return ResponseEntity.ok().build();
    }
}
