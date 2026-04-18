package com.shop.sehodiary_api.repository.comment;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Repository
public class CommentIdRedisRepository {

    private static final String DIARY_ID_KEY = "comments:diary:";
    private static final String USER_ID_KEY = "comments:user:";
    private static final Duration TTL = Duration.ofDays(1);

    private final RedisTemplate<String, Long> redisTemplate;

    public CommentIdRedisRepository(
            @Qualifier("longRedisTemplate")
            RedisTemplate<String, Long> redisTemplate
    ) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * diary 기준 댓글 ID 추가
     * score: 정렬용 값 (보통 createdAt epoch milli)
     */
    public void addByDiaryId(Long diaryId, Long commentId, double score) {
        if (diaryId == null || commentId == null) {
            return;
        }

        String key = generateDiaryKey(diaryId);
        redisTemplate.opsForZSet().add(key, commentId, score);
        redisTemplate.expire(key, TTL);
    }

    /**
     * user 기준 댓글 ID 추가
     * score: 정렬용 값 (보통 createdAt epoch milli)
     */
    public void addByUserId(Long userId, Long commentId, double score) {
        if (userId == null || commentId == null) {
            return;
        }

        String key = generateUserKey(userId);
        redisTemplate.opsForZSet().add(key, commentId, score);
        redisTemplate.expire(key, TTL);
    }

    /**
     * diary 기준 전체 저장
     * commentIds는 정렬된 상태라고 가정하고 index를 score로 사용
     * 더 정확하게 하려면 (commentId, score) 쌍으로 받는 구조가 더 좋음
     */
    public void saveAllByDiaryId(Long diaryId, List<Long> commentIds) {
        if (diaryId == null || commentIds == null || commentIds.isEmpty()) {
            return;
        }

        String key = generateDiaryKey(diaryId);
        redisTemplate.delete(key);

        for (int i = 0; i < commentIds.size(); i++) {
            Long commentId = commentIds.get(i);
            redisTemplate.opsForZSet().add(key, commentId, i);
        }

        redisTemplate.expire(key, TTL);
    }

    /**
     * user 기준 전체 저장
     */
    public void saveAllByUserId(Long userId, List<Long> commentIds) {
        if (userId == null || commentIds == null || commentIds.isEmpty()) {
            return;
        }

        String key = generateUserKey(userId);
        redisTemplate.delete(key);

        for (int i = 0; i < commentIds.size(); i++) {
            Long commentId = commentIds.get(i);
            redisTemplate.opsForZSet().add(key, commentId, i);
        }

        redisTemplate.expire(key, TTL);
    }

    /**
     * diary 기준 전체 조회 (오래된 순)
     */
    public List<Long> findAllByDiaryId(Long diaryId) {
        if (diaryId == null) {
            return Collections.emptyList();
        }

        Set<Long> ids = redisTemplate.opsForZSet().range(generateDiaryKey(diaryId), 0, -1);
        return ids == null ? Collections.emptyList() : new ArrayList<>(ids);
    }

    /**
     * diary 기준 전체 조회 (최신순)
     */
    public List<Long> findAllByDiaryIdDesc(Long diaryId) {
        if (diaryId == null) {
            return Collections.emptyList();
        }

        Set<Long> ids = redisTemplate.opsForZSet().reverseRange(generateDiaryKey(diaryId), 0, -1);
        return ids == null ? Collections.emptyList() : new ArrayList<>(ids);
    }

    /**
     * user 기준 전체 조회 (오래된 순)
     */
    public List<Long> findAllByUserId(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        Set<Long> ids = redisTemplate.opsForZSet().range(generateUserKey(userId), 0, -1);
        return ids == null ? Collections.emptyList() : new ArrayList<>(ids);
    }

    /**
     * user 기준 전체 조회 (최신순)
     */
    public List<Long> findAllByUserIdDesc(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        Set<Long> ids = redisTemplate.opsForZSet().reverseRange(generateUserKey(userId), 0, -1);
        return ids == null ? Collections.emptyList() : new ArrayList<>(ids);
    }

    public void removeByDiaryId(Long diaryId, Long commentId) {
        if (diaryId == null || commentId == null) {
            return;
        }

        redisTemplate.opsForZSet().remove(generateDiaryKey(diaryId), commentId);
    }

    public void removeByUserId(Long userId, Long commentId) {
        if (userId == null || commentId == null) {
            return;
        }

        redisTemplate.opsForZSet().remove(generateUserKey(userId), commentId);
    }

    public void deleteByDiaryId(Long diaryId) {
        if (diaryId == null) {
            return;
        }

        redisTemplate.delete(generateDiaryKey(diaryId));
    }

    public void deleteByUserId(Long userId) {
        if (userId == null) {
            return;
        }

        redisTemplate.delete(generateUserKey(userId));
    }

    private String generateDiaryKey(Long diaryId) {
        return DIARY_ID_KEY + diaryId;
    }

    private String generateUserKey(Long userId) {
        return USER_ID_KEY + userId;
    }

    public boolean existsDiaryKey(Long diaryId) { return redisTemplate.hasKey(DIARY_ID_KEY + diaryId); }
    public boolean existsUserKey(Long userId) {
        return redisTemplate.hasKey(USER_ID_KEY + userId);
    }
}