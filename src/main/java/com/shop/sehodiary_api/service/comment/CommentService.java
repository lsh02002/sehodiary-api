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
        List<Long> commentIds = commentIdRedisRepository.findAllByDiaryId(diaryId);
        Map<Long, CommentResponse> cached = commentCacheRepository.getAll();

        if (commentIds.isEmpty()) {
            List<Long> ids = commentRepository.findAllIdsByDiaryId(diaryId);

            commentIdRedisRepository.saveAllByDiaryId(diaryId, ids);
            commentIds = ids;
        }

        List<CommentResponse> result = commentIds.stream()
                .map(cached::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));

        List<Long> missingIds = commentIds.stream()
                .filter(id -> !cached.containsKey(id))
                .toList();

        if (!missingIds.isEmpty()) {
            List<CommentResponse> dbResults = commentRepository.findAllById(missingIds).stream()
                    .map(commentMapper::toResponse)
                    .toList();

            dbResults.forEach(commentCacheRepository::put);
            result.addAll(dbResults);
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByUser(Long userId) {

        List<Long> commentIds = commentIdRedisRepository.findAllByUserId(userId);
        Map<Long, CommentResponse> cached = commentCacheRepository.getAll();

        // Redis에 없으면 DB에서 채우기
        if (commentIds.isEmpty()) {
            List<Long> ids = commentRepository.findIdsByUserId(userId);

            commentIdRedisRepository.saveAllByUserId(userId, ids);
            commentIds = ids;
        }

        List<CommentResponse> result = new ArrayList<>();
        List<Long> missingIds = new ArrayList<>();

        for (Long commentId : commentIds) {
            CommentResponse cachedComment = cached.get(commentId);

            if (cachedComment != null) {
                result.add(cachedComment);
            } else {
                missingIds.add(commentId);
            }
        }

        if (!missingIds.isEmpty()) {
            List<CommentResponse> dbResults = commentRepository.findAllById(missingIds).stream()
                    .map(commentMapper::toResponse)
                    .toList();

            dbResults.forEach(commentCacheRepository::put);
            result.addAll(dbResults);
        }

        return result;
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
        commentIdRedisRepository.addByDiaryId(savedComment.getDiary().getId(), savedComment.getId());
        commentIdRedisRepository.addByUserId(userId, savedComment.getId());

        return commentResponse;
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

            Comment comment = commentRepository.findByUserIdAndId(userId, commentId)
                    .orElseThrow(()->new NotFoundException("해당 사용자의 댓글이 아닙니다.", commentId));

            Object beforecomment = snapshotFunc.snapshot(comment);
            activityLogService.log(ActivityEntityType.COMMENT, ActivityAction.DELETE, comment.getId(), comment.logMessage(), user, beforecomment, null);

            comment.getDiary().removeComment(comment);
            DiaryResponse response = diaryMapper.toResponse(comment.getDiary());
            diaryCacheRepository.put(response);

            commentCacheRepository.evict(commentId);
            commentIdRedisRepository.removeByDiaryId(comment.getDiary().getId(), commentId);
            commentIdRedisRepository.removeByUserId(userId, commentId);

            commentRepository.delete(comment);
        } catch (Exception e) {
            throw new ConflictException("글 삭제를 실패했습니다.", commentId);
        }
    }
}
