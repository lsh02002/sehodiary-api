package com.shop.sehodiary_api.web.controller.diarysearch;

import com.shop.sehodiary_api.repository.user.userDetails.CustomUserDetails;
import com.shop.sehodiary_api.service.diarysearch.DiarySearchIndexer;
import com.shop.sehodiary_api.service.diarysearch.DiarySearchService;
import com.shop.sehodiary_api.web.dto.diary.DiaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/diary")
public class DiarySearchController {

    private final DiarySearchService diarySearchService;
    private final DiarySearchIndexer diarySearchIndexer;

    @GetMapping("/public/search")
    public ResponseEntity<Page<DiaryResponse>> getDiariesByPublic(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String keyword,
            Pageable pageable
    ) {
        Long userId = userDetails == null ? null : userDetails.getId();

        Page<DiaryResponse> response =
                diarySearchService.getDiariesByPublic(userId, keyword, pageable);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/friends/{targetUserId}/search")
    public ResponseEntity<Page<DiaryResponse>> getDiariesPublicAndFriendsByUser(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long targetUserId,
            @RequestParam(required = false) String keyword,
            Pageable pageable
    ) {
        Long userId = userDetails == null ? null : userDetails.getId();

        Page<DiaryResponse> response =
                diarySearchService.getDiariesPublicAndFriendsByUser(
                        userId,
                        targetUserId,
                        keyword,
                        pageable
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/rebuild/search")
    public ResponseEntity<Void> rebuildDiarySearchDocuments() {
        diarySearchIndexer.rebuildDiarySearchDocuments();
        return ResponseEntity.ok().build();
    }
}
