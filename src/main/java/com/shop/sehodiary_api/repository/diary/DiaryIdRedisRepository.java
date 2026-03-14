package com.shop.sehodiary_api.repository.diary;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class DiaryIdRedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String PUBLIC_IDS_KEY = "diary:ids:public";
    private static final String FRIENDS_IDS_KEY = "diary:ids:friends";

    public void addPublic(Long diaryId) {
        redisTemplate.opsForSet().add(PUBLIC_IDS_KEY, diaryId);
    }

    public void addFriends(Long diaryId) {
        redisTemplate.opsForSet().add(FRIENDS_IDS_KEY, diaryId);
    }

    public void removePublic(Long diaryId) {
        redisTemplate.opsForSet().remove(PUBLIC_IDS_KEY, diaryId);
    }

    public void removeFriends(Long diaryId) {
        redisTemplate.opsForSet().remove(FRIENDS_IDS_KEY, diaryId);
    }

    public Set<Long> findAllPublic() {
        return getLongSet(PUBLIC_IDS_KEY);
    }

    public Set<Long> findAllFriends() {
        return getLongSet(FRIENDS_IDS_KEY);
    }

    public void savePublicIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        redisTemplate.opsForSet().add(PUBLIC_IDS_KEY, ids.toArray(new Long[0]));
    }

    public void saveFriends(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        redisTemplate.opsForSet().add(FRIENDS_IDS_KEY, ids.toArray(new Long[0]));
    }

    private Set<Long> getLongSet(String key) {
        Set<Object> members = redisTemplate.opsForSet().members(key);

        if (members == null || members.isEmpty()) {
            return Collections.emptySet();
        }

        return members.stream()
                .map(v -> Long.valueOf(String.valueOf(v)))
                .collect(Collectors.toSet());
    }

    public void remove(Long diaryId) {
        redisTemplate.opsForSet().remove(PUBLIC_IDS_KEY, diaryId);
        redisTemplate.opsForSet().remove(FRIENDS_IDS_KEY, diaryId);
    }
}
