package com.shop.sehodiary_api.web.controller.comment;

import com.shop.sehodiary_api.repository.user.userDetails.CustomUserDetails;
import com.shop.sehodiary_api.service.comment.CommentService;
import com.shop.sehodiary_api.web.dto.comment.CommentRequest;
import com.shop.sehodiary_api.web.dto.comment.CommentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comment")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @GetMapping("/diary/{diaryId}")
    public ResponseEntity<List<CommentResponse>> getCommentsByDiaryId(@PathVariable Long diaryId) {
        return ResponseEntity.ok(commentService.getCommentsByDiaryId(diaryId));
    }

    @GetMapping("/user")
    public ResponseEntity<List<CommentResponse>> getCommentsByUser(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        return ResponseEntity.ok(commentService.getCommentsByUser(customUserDetails.getId()));
    }

    @PostMapping("/create")
    public ResponseEntity<CommentResponse> createComment(@AuthenticationPrincipal CustomUserDetails customUserDetails, @RequestBody CommentRequest request) {
        return ResponseEntity.ok(commentService.createComment(customUserDetails.getId(), request));
    }

    @PostMapping("/{commentId}")
    public ResponseEntity<CommentResponse> editComment(@AuthenticationPrincipal CustomUserDetails customUserDetails, @PathVariable Long commentId, @RequestBody CommentRequest request) {
        return ResponseEntity.ok(commentService.editComment(customUserDetails.getId(), commentId, request));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(@AuthenticationPrincipal CustomUserDetails customUserDetails, @PathVariable Long commentId) {
        commentService.deleteComment(customUserDetails.getId(), commentId);
        return ResponseEntity.ok().build();
    }
}
