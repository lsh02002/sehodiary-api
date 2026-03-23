package com.shop.sehodiary_api.repository.comment;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

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

    public void addByDiaryId(Long diaryId, Long commentId) {
        if (diaryId == null || commentId == null) {
            return;
        }

        String key = generateDiaryKey(diaryId);
        redisTemplate.opsForList().rightPush(key, commentId);
        redisTemplate.expire(key, TTL);
    }

    public void addByUserId(Long userId, Long commentId) {
        if (userId == null || commentId == null) {
            return;
        }

        String key = generateUserKey(userId);
        redisTemplate.opsForList().rightPush(key, commentId);
        redisTemplate.expire(key, TTL);
    }

    public void saveAllByDiaryId(Long diaryId, List<Long> commentIds) {
        if (diaryId == null || commentIds == null || commentIds.isEmpty()) {
            return;
        }

        String key = generateDiaryKey(diaryId);
        redisTemplate.delete(key);
        redisTemplate.opsForList().rightPushAll(key, commentIds);
        redisTemplate.expire(key, TTL);
    }

    public void saveAllByUserId(Long userId, List<Long> commentIds) {
        if (userId == null || commentIds == null || commentIds.isEmpty()) {
            return;
        }

        String key = generateUserKey(userId);
        redisTemplate.delete(key);
        redisTemplate.opsForList().rightPushAll(key, commentIds);
        redisTemplate.expire(key, TTL);
    }

    public List<Long> findAllByDiaryId(Long diaryId) {
        if (diaryId == null) {
            return Collections.emptyList();
        }

        List<Long> ids = redisTemplate.opsForList().range(generateDiaryKey(diaryId), 0, -1);
        return ids == null ? Collections.emptyList() : ids;
    }

    public List<Long> findAllByUserId(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        List<Long> ids = redisTemplate.opsForList().range(generateUserKey(userId), 0, -1);
        return ids == null ? Collections.emptyList() : ids;
    }

    public void removeByDiaryId(Long diaryId, Long commentId) {
        if (diaryId == null || commentId == null) {
            return;
        }

        redisTemplate.opsForList().remove(generateDiaryKey(diaryId), 1, commentId);
    }

    public void removeByUserId(Long userId, Long commentId) {
        if (userId == null || commentId == null) {
            return;
        }

        redisTemplate.opsForList().remove(generateUserKey(userId), 1, commentId);
    }

    private String generateDiaryKey(Long diaryId) {
        return DIARY_ID_KEY + diaryId;
    }

    private String generateUserKey(Long userId) {
        return USER_ID_KEY + userId;
    }
}