package com.shop.sehodiary_api.service.comment;

import com.shop.sehodiary_api.repository.activity.function.SnapshotFunc;
import com.shop.sehodiary_api.repository.activity.ActivityAction;
import com.shop.sehodiary_api.repository.activity.ActivityEntityType;
import com.shop.sehodiary_api.repository.comment.Comment;
import com.shop.sehodiary_api.repository.comment.CommentRepository;
import com.shop.sehodiary_api.repository.diary.Diary;
import com.shop.sehodiary_api.repository.diary.DiaryRepository;
import com.shop.sehodiary_api.repository.user.User;
import com.shop.sehodiary_api.repository.user.UserRepository;
import com.shop.sehodiary_api.service.activelog.ActivityLogService;
import com.shop.sehodiary_api.service.exceptions.BadRequestException;
import com.shop.sehodiary_api.service.exceptions.ConflictException;
import com.shop.sehodiary_api.service.exceptions.NotFoundException;
import com.shop.sehodiary_api.web.dto.comment.CommentRequest;
import com.shop.sehodiary_api.web.dto.comment.CommentResponse;
import com.shop.sehodiary_api.web.mapper.comment.CommentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final UserRepository userRepository;
    private final DiaryRepository diaryRepository;
    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final ActivityLogService activityLogService;
    private final SnapshotFunc snapshotFunc;

    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByDiaryId(Long diaryId){
        return commentRepository.findByDiaryId(diaryId)
                .stream().map(commentMapper::toResponse).toList();
    }

    @Transactional
    public List<CommentResponse> getCommentsByUser(Long userId) {
        return commentRepository.findByUserId(userId)
                .stream().map(commentMapper::toResponse).toList();
    }

    @Transactional
    public CommentResponse createComment(Long userId, CommentRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(()->new NotFoundException("입력하신 아이디로 회원을 찾을 수 없습니다.", userId));

        Diary diary = diaryRepository.findById(request.getDiaryId())
                .orElseThrow(()->new NotFoundException("입력하신 아이디로 게시물을 찾을 수 없습니다.", request.getDiaryId()));

        if(request.getContent().trim().isEmpty()) {
            throw new BadRequestException("댓글 내용란이 공란입니다", null);
        }

        Comment comment = Comment.builder()
                .user(user)
                .diary(diary)
                .content(request.getContent())
                .build();

        commentRepository.save(comment);

        Object afterComment = snapshotFunc.snapshot(comment);

        activityLogService.log(ActivityEntityType.COMMENT, ActivityAction.CREATE, comment.getId(), comment.logMessage(), user, null, afterComment);

        return commentMapper.toResponse(comment);
    }

    @Transactional
    public CommentResponse editComment(Long userId, Long commentId, CommentRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(()->new NotFoundException("해당 사용자를 찾을 수 없습니다.", userId));

        Comment comment = commentRepository.findByUserIdAndId(userId, commentId)
                .orElseThrow(()->new NotFoundException("해당 사용자의 댓글이 아닙니다.", commentId));

        Object beforecomment = snapshotFunc.snapshot(comment);

        if(request.getContent().trim().isEmpty()) {
            throw new ConflictException("내용란이 비어있습니다.", null);
        }

        comment.setContent(request.getContent());

        Object aftercomment = snapshotFunc.snapshot(comment);

        activityLogService.log(ActivityEntityType.COMMENT, ActivityAction.UPDATE, comment.getId(), comment.logMessage(), user, beforecomment, aftercomment);

        return commentMapper.toResponse(comment);
    }

    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(()->new NotFoundException("해당 사용자를 찾을 수 없습니다.", userId));

            Comment comment = commentRepository.findByUserIdAndId(userId, commentId)
                    .orElseThrow(()->new NotFoundException("해당 사용자의 댓글이 아닙니다.", commentId));

            Object beforecomment = snapshotFunc.snapshot(comment);

            activityLogService.log(ActivityEntityType.COMMENT, ActivityAction.DELETE, comment.getId(), comment.logMessage(), user, beforecomment, null);

            commentRepository.delete(comment);
        } catch (Exception e) {
            throw new ConflictException("글 삭제를 실패했습니다.", commentId);
        }
    }
}
