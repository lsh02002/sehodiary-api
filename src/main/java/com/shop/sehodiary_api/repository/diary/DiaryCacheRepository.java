package com.shop.sehodiary_api.repository.diary;

import com.shop.sehodiary_api.repository.common.Visibility;
import com.shop.sehodiary_api.web.dto.diary.DiaryResponse;
import com.shop.sehodiary_api.web.mapper.diary.DiaryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class DiaryCacheRepository {
    private final DiaryRepository diaryRepository;
    private final DiaryMapper diaryMapper;

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String KEY = "diary:cache";

    public Optional<DiaryResponse> get(Long diaryId) {
        Object value = redisTemplate.opsForHash().get(KEY, diaryId);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of((DiaryResponse) value);
    }

    public void put(DiaryResponse response) {
        redisTemplate.opsForHash().put(KEY, response.getId(), response);
    }

    public void delete(Long diaryId) {
        redisTemplate.opsForHash().delete(KEY, diaryId);
    }

    public Map<Long, DiaryResponse> getAll() {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(KEY);

        if (entries.isEmpty()) {
            List<Diary> diaries = diaryRepository.findAllWithUser();

            Map<Object, Object> map = diaries.stream()
                    .collect(Collectors.toMap(
                            Diary::getId,
                            diaryMapper::toResponse
                    ));

            redisTemplate.opsForHash().putAll(KEY, map);
            entries = map;
        }

        return entries.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> Long.valueOf(String.valueOf(e.getKey())),
                        e -> (DiaryResponse) e.getValue()
                ));
    }
}