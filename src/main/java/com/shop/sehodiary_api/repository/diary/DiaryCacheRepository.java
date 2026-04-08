package com.shop.sehodiary_api.repository.diary;

import com.shop.sehodiary_api.web.dto.diary.DiaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@RequiredArgsConstructor
public class DiaryCacheRepository {

    private static final String DIARY_CACHE_KEY = "diary:cache:";

    private final DiaryRepository diaryRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public Optional<DiaryResponse> get(Long diaryId) {
        Object value = redisTemplate.opsForHash().get(DIARY_CACHE_KEY, diaryId);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of((DiaryResponse) value);
    }

    public void put(DiaryResponse response) {
        redisTemplate.opsForHash().put(DIARY_CACHE_KEY, response.getId(), response);
    }

    public void delete(Long diaryId) {
        redisTemplate.opsForHash().delete(DIARY_CACHE_KEY, diaryId);
    }

    public Map<Long, DiaryResponse> getAll() {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(DIARY_CACHE_KEY);

        if (entries.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, DiaryResponse> result = new HashMap<>();

        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            DiaryResponse value = (DiaryResponse) entry.getValue();
            if (value != null && value.getId() != null) {
                result.put(value.getId(), value);
            }
        }

        return result;
    }

    public void evictDiaryCacheByUser(Long userId) {
        List<Long> diaryIds = diaryRepository.findIdsByUserId(userId);

        if (!diaryIds.isEmpty()) {
            redisTemplate.opsForHash().delete(
                    DIARY_CACHE_KEY,
                    diaryIds.toArray(new Object[0])
            );
        }
    }

    public Map<Long, DiaryResponse> getAllByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Object> keys = ids.stream()
                .map(id -> (Object) id)
                .toList();

        List<Object> values = redisTemplate.opsForHash()
                .multiGet(DIARY_CACHE_KEY, keys);

        Map<Long, DiaryResponse> result = new HashMap<>();

        for (int i = 0; i < ids.size(); i++) {
            DiaryResponse value = (DiaryResponse) values.get(i);
            if (value != null) {
                result.put(ids.get(i), value);
            }
        }

        return result;
    }
}