package com.shop.sehodiary_api.repository.comment;

import com.shop.sehodiary_api.web.dto.comment.CommentResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.*;

@Repository
public class CommentCacheRepository {
    private final RedisTemplate<String, CommentResponse> redisTemplate;

    private static final String KEY_PREFIX = "comment:";
    private static final Duration TTL = Duration.ofDays(1);
    private final CommentRepository commentRepository;

    public CommentCacheRepository(
            @Qualifier("commentRedisTemplate")
            RedisTemplate<String, CommentResponse> redisTemplate,
            CommentRepository commentRepository) {
        this.redisTemplate = redisTemplate;
        this.commentRepository = commentRepository;
    }

    public void put(CommentResponse response) {
        if (response == null || response.getCommentId() == null) {
            return;
        }

        redisTemplate.opsForValue().set(
                generateKey(response.getCommentId()),
                response,
                TTL
        );
    }

    public Optional<CommentResponse> get(Long commentId) {
        if (commentId == null) {
            return Optional.empty();
        }

        CommentResponse response = redisTemplate.opsForValue().get(generateKey(commentId));
        return Optional.ofNullable(response);
    }

    public void evict(Long commentId) {
        if (commentId == null) {
            return;
        }

        redisTemplate.delete(generateKey(commentId));
    }

    private String generateKey(Long commentId) {
        return KEY_PREFIX + commentId;
    }

    public Map<Long, CommentResponse> getAll() {
        Set<String> keys = redisTemplate.keys("comment:*");

        if (keys.isEmpty()) {
            return Collections.emptyMap();
        }

        List<CommentResponse> values = redisTemplate.opsForValue().multiGet(keys);

        Map<Long, CommentResponse> result = new HashMap<>();
        List<String> keyList = new ArrayList<>(keys);

        for (int i = 0; i < keyList.size(); i++) {
            CommentResponse value = Objects.requireNonNull(values).get(i);
            if (value != null && value.getCommentId() != null) {
                result.put(value.getCommentId(), value);
            }
        }

        return result;
    }

    public void evictCommentCacheByUser(Long userId) {
        List<Long> commentIds = commentRepository.findIdsByUserId(userId);

        List<String> keys = commentIds.stream()
                .map(this::generateKey)
                .toList();

        redisTemplate.delete(keys);
    }
}
