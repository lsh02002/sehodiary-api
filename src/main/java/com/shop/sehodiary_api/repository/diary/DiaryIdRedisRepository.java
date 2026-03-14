package com.shop.sehodiary_api.repository.diary;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class DiaryIdRedisRepository {
    private final DiaryRepository diaryRepository;

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String KEY = "diary:ids";

    public void add(Long diaryId) {
        redisTemplate.opsForSet().add(KEY, diaryId);
    }

    public void remove(Long diaryId) {
        redisTemplate.opsForSet().remove(KEY, diaryId);
    }

    public Set<Long> findAll() {
        Set<Object> members = redisTemplate.opsForSet().members(KEY);

        if (members == null || diaryRepository.count() != members.size()) {
            List<Long> ids = diaryRepository.findAllIds();

            if (!ids.isEmpty()) {
                Set<Long> missingIds = new HashSet<>(ids);

                if (members != null) {
                    missingIds.removeAll(members); // members에 없는 id만 남김
                }

                if (!missingIds.isEmpty()) {
                    redisTemplate.opsForSet().add(KEY, missingIds.toArray());
                }
            }

            return new HashSet<>(ids);
        }

        return members.stream()
                .map(v -> Long.valueOf(String.valueOf(v)))
                .collect(Collectors.toSet());
    }
}