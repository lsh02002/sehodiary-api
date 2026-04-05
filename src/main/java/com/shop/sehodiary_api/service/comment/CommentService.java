package com.shop.sehodiary_api.service.comment;

import com.shop.sehodiary_api.repository.activity.function.SnapshotFunc;
import com.shop.sehodiary_api.repository.activity.ActivityAction;
import com.shop.sehodiary_api.repository.activity.ActivityEntityType;
import com.shop.sehodiary_api.repository.comment.Comment;
import com.shop.sehodiary_api.repository.comment.CommentCacheRepository;
import com.shop.sehodiary_api.repository.comment.CommentIdRedisRepository;
import com.shop.sehodiary_api.repository.comment.CommentRepository;
import com.shop.sehodiary_api.repository.diary.Diary;
import com.shop.sehodiary_api.repository.diary.DiaryCacheRepository;
import com.shop.sehodiary_api.repository.diary.DiaryRepository;
import com.shop.sehodiary_api.repository.user.User;
import com.shop.sehodiary_api.repository.user.UserRepository;
import com.shop.sehodiary_api.service.activelog.ActivityLogService;
import com.shop.sehodiary_api.service.exceptions.BadRequestException;
import com.shop.sehodiary_api.service.exceptions.ConflictException;
import com.shop.sehodiary_api.service.exceptions.NotFoundException;
import com.shop.sehodiary_api.web.dto.comment.CommentRequest;
import com.shop.sehodiary_api.web.dto.comment.CommentResponse;
import com.shop.sehodiary_api.web.dto.diary.DiaryResponse;
import com.shop.sehodiary_api.web.mapper.comment.CommentMapper;
import com.shop.sehodiary_api.web.mapper.diary.DiaryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final UserRepository userRepository;
    private final DiaryRepository diaryRepository;
    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final ActivityLogService activityLogService;
    private final SnapshotFunc snapshotFunc;

    private final DiaryCacheRepository diaryCacheRepository;
    private final DiaryMapper diaryMapper;

    private final CommentCacheRepository commentCacheRepository;
    private final CommentIdRedisRepository commentIdRedisRepository;

    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByDiaryId(Long diaryId) {
        List<Long> commentIds = commentIdRedisRepository.findAllByDiaryIdDesc(diaryId);

        if (commentIds.isEmpty()) {
            commentIds = commentRepository.findAllIdsByDiaryIdDesc(diaryId);
            commentIdRedisRepository.saveAllByDiaryId(diaryId, commentIds);
        }

        Map<Long, CommentResponse> cachedComments = new HashMap<>(commentCacheRepository.getAll());

        List<Long> missingIds = commentIds.stream()
                .filter(id -> !cachedComments.containsKey(id))
                .toList();

        if (!missingIds.isEmpty()) {
            Map<Long, CommentResponse> dbComments = commentRepository.findAllById(missingIds).stream()
                    .map(commentMapper::toResponse)
                    .collect(Collectors.toMap(
                            CommentResponse::getCommentId,
                            response -> response
                    ));

            dbComments.values().forEach(commentCacheRepository::put);
            cachedComments.putAll(dbComments);
        }

        return commentIds.stream()
                .map(cachedComments::get)
                .filter(Objects::nonNull)
                .toList();
    }

    public List<CommentResponse> getCommentsByUser(Long userId) {
        List<Long> commentids = commentIdRedisRepository.findAllByUserIdDesc(userId);

        if(commentids.isEmpty()) {
            commentids = commentRepository.findAllIdsByUserIdDesc(userId);
            commentIdRedisRepository.saveAllByUserId(userId, commentids);
        }

        Map<Long, CommentResponse> cachedComments = new HashMap<>(commentCacheRepository.getAll());

        List<Long> missingIds = commentids.stream()
                .filter(id -> !cachedComments.containsKey(id))
                .toList();

        if (!missingIds.isEmpty()) {
            Map<Long, CommentResponse> dbComments = commentRepository.findAllById(missingIds).stream()
                    .map(commentMapper::toResponse)
                    .collect(Collectors.toMap(
                            CommentResponse::getCommentId,
                            response->response
                    ));

            dbComments.values().forEach(commentCacheRepository::put);
            cachedComments.putAll(dbComments);
        }

        return commentids.stream()
                .map(cachedComments::get)
                .filter(Objects::nonNull)
                .toList();
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

        Comment savedComment = commentRepository.save(comment);

        Object afterComment = snapshotFunc.snapshot(savedComment);
        activityLogService.log(ActivityEntityType.COMMENT, ActivityAction.CREATE, savedComment.getId(), savedComment.logMessage(), user, null, afterComment);

        diary.getComments().add(savedComment);

        DiaryResponse response = diaryMapper.toResponse(diary);
        diaryCacheRepository.put(response);

        CommentResponse commentResponse = commentMapper.toResponse(savedComment);
        commentCacheRepository.put(commentResponse);
        double score = savedComment.getCreatedAt()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();

        commentIdRedisRepository.addByDiaryId(
                savedComment.getDiary().getId(),
                savedComment.getId(),
                score
        );

        commentIdRedisRepository.addByUserId(
                userId,
                savedComment.getId(),
                score
        );

        return commentResponse;
    }

    @Transactional
    public CommentResponse editComment(Long userId, Long commentId, CommentRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(()->new NotFoundException("해당 사용자를 찾을 수 없습니다.", userId));

        commentRepository.findById(commentId)
                .orElseThrow(()->new NotFoundException("해당 댓글을 찾을 수 없습니다.", commentId));

        Comment comment = commentRepository.findByUserIdAndId(userId, commentId)
                .orElseThrow(()->new NotFoundException("해당 사용자의 댓글이 아닙니다.", commentId));

        Object beforecomment = snapshotFunc.snapshot(comment);

        if(request.getContent().trim().isEmpty()) {
            throw new ConflictException("내용란이 비어있습니다.", null);
        }

        comment.setContent(request.getContent());
        Object aftercomment = snapshotFunc.snapshot(comment);
        activityLogService.log(ActivityEntityType.COMMENT, ActivityAction.UPDATE, comment.getId(), comment.logMessage(), user, beforecomment, aftercomment);

        comment.getDiary().addComment(comment);
        DiaryResponse response = diaryMapper.toResponse(comment.getDiary());
        diaryCacheRepository.put(response);

        CommentResponse commentResponse = commentMapper.toResponse(comment);
        commentCacheRepository.put(commentResponse);

        return commentResponse;
    }

    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(()->new NotFoundException("해당 사용자를 찾을 수 없습니다.", userId));

            commentRepository.findById(commentId)
                    .orElseThrow(()->new NotFoundException("해당 댓글을 찾을 수 없습니다.", commentId));

            Comment comment = commentRepository.findByUserIdAndId(userId, commentId)
                    .orElseThrow(()->new NotFoundException("해당 사용자의 댓글이 아닙니다.", commentId));

            Object beforecomment = snapshotFunc.snapshot(comment);
            activityLogService.log(ActivityEntityType.COMMENT, ActivityAction.DELETE, comment.getId(), comment.logMessage(), user, beforecomment, null);

            Diary diary = comment.getDiary();
            diary.removeComment(comment);

            DiaryResponse response = diaryMapper.toResponse(diary);
            diaryCacheRepository.put(response);

            commentCacheRepository.evict(commentId);
            commentIdRedisRepository.removeByDiaryId(diary.getId(), commentId);
            commentIdRedisRepository.removeByUserId(userId, commentId);

            commentRepository.delete(comment);
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new ConflictException("글 삭제를 실패했습니다.", commentId);
        }
    }
}
