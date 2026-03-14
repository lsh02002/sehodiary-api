package com.shop.sehodiary_api.repository.diary;

import com.shop.sehodiary_api.web.dto.diary.DiaryResponse;
import com.shop.sehodiary_api.web.mapper.diary.DiaryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class DiaryCacheRepository {

    private static final String DIARY_CACHE_KEY = "diary:cache";
    private static final String PUBLIC_IDS_KEY = "diary:ids:public";
    private static final String FRIENDS_IDS_KEY = "diary:ids:friends";

    private final DiaryRepository diaryRepository;
    private final DiaryMapper diaryMapper;
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
        redisTemplate.opsForSet().remove(PUBLIC_IDS_KEY, diaryId);
        redisTemplate.opsForSet().remove(FRIENDS_IDS_KEY, diaryId);
    }

    public Map<Long, DiaryResponse> getAll() {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(DIARY_CACHE_KEY);

        if (entries.isEmpty()) {
            List<Diary> diaries = diaryRepository.findAllWithUser();

            Map<Object, Object> map = diaries.stream()
                    .collect(Collectors.toMap(
                            Diary::getId,
                            diaryMapper::toResponse
                    ));

            if (!map.isEmpty()) {
                redisTemplate.opsForHash().putAll(DIARY_CACHE_KEY, map);
            }
            entries = map;
        }

        return entries.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> Long.valueOf(String.valueOf(e.getKey())),
                        e -> (DiaryResponse) e.getValue()
                ));
    }

    public Set<Long> findAllPublic() {
        return findIds(
                PUBLIC_IDS_KEY,
                diaryRepository.findAllPublicIds()
        );
    }

    public Set<Long> findAllFriends() {
        return findIds(
                FRIENDS_IDS_KEY,
                diaryRepository.findAllFriendsIds()
        );
    }

    private Set<Long> findIds(String key, List<Long> fallbackIds) {
        Set<Object> members = redisTemplate.opsForSet().members(key);

        if (members == null || members.isEmpty()) {
            if (!fallbackIds.isEmpty()) {
                redisTemplate.opsForSet().add(key, fallbackIds.toArray());
            }
            return new HashSet<>(fallbackIds);
        }

        return members.stream()
                .map(v -> Long.valueOf(String.valueOf(v)))
                .collect(Collectors.toSet());
    }
}