package com.shop.sehodiary_api.repository.diary;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class DiaryIdRedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String PUBLIC_IDS_KEY = "diary:ids:public";
    private static final String FRIENDS_IDS_KEY = "diary:ids:friends";
    private static final String USER_IDS_KEY_PREFIX = "diary:ids:user:";

    public void addPublic(Long diaryId) {
        if (diaryId == null) {
            return;
        }
        redisTemplate.opsForZSet().add(PUBLIC_IDS_KEY, diaryId, diaryId.doubleValue());
    }

    public void addFriends(Long diaryId) {
        if (diaryId == null) {
            return;
        }
        redisTemplate.opsForZSet().add(FRIENDS_IDS_KEY, diaryId, diaryId.doubleValue());
    }

    public void addUser(Long userId, Long diaryId) {
        if (userId == null || diaryId == null) {
            return;
        }
        redisTemplate.opsForZSet().add(getUserIdsKey(userId), diaryId, diaryId.doubleValue());
    }

    public void removePublic(Long diaryId) {
        if (diaryId == null) {
            return;
        }
        redisTemplate.opsForZSet().remove(PUBLIC_IDS_KEY, diaryId);
    }

    public void removeFriends(Long diaryId) {
        if (diaryId == null) {
            return;
        }
        redisTemplate.opsForZSet().remove(FRIENDS_IDS_KEY, diaryId);
    }

    public void removeUser(Long userId, Long diaryId) {
        if (userId == null || diaryId == null) {
            return;
        }
        redisTemplate.opsForZSet().remove(getUserIdsKey(userId), diaryId);
    }

    public List<Long> findAllPublic() {
        return getLongListDesc(PUBLIC_IDS_KEY);
    }

    public List<Long> findAllFriends() {
        return getLongListDesc(FRIENDS_IDS_KEY);
    }

    public List<Long> findAllUser(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return getLongListDesc(getUserIdsKey(userId));
    }

    public void savePublicIds(List<Long> ids) {
        saveAll(PUBLIC_IDS_KEY, ids);
    }

    public void saveFriends(List<Long> ids) {
        saveAll(FRIENDS_IDS_KEY, ids);
    }

    public void saveUserIds(Long userId, List<Long> ids) {
        if (userId == null) {
            return;
        }
        saveAll(getUserIdsKey(userId), ids);
    }

    private void saveAll(String key, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        Set<ZSetOperations.TypedTuple<Object>> tuples = new java.util.LinkedHashSet<>();
        for (Long id : ids) {
            if (id != null) {
                tuples.add(ZSetOperations.TypedTuple.of(id, id.doubleValue()));
            }
        }

        if (!tuples.isEmpty()) {
            redisTemplate.opsForZSet().add(key, tuples);
        }
    }

    private List<Long> getLongListDesc(String key) {
        Set<Object> members = redisTemplate.opsForZSet().reverseRange(key, 0, -1);

        if (members == null || members.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> result = new ArrayList<>(members.size());
        for (Object member : members) {
            result.add(Long.valueOf(String.valueOf(member)));
        }
        return result;
    }

    private String getUserIdsKey(Long userId) {
        return USER_IDS_KEY_PREFIX + userId;
    }

    public void remove(Long diaryId) {
        if (diaryId == null) {
            return;
        }
        redisTemplate.opsForZSet().remove(PUBLIC_IDS_KEY, diaryId);
        redisTemplate.opsForZSet().remove(FRIENDS_IDS_KEY, diaryId);
    }

    public void removeFromUser(Long userId, Long diaryId) {
        if (userId == null || diaryId == null) {
            return;
        }
        redisTemplate.opsForZSet().remove(getUserIdsKey(userId), diaryId);
    }

    //테스트 여기부터
    public boolean existsPublicKey() {
        return redisTemplate.hasKey(PUBLIC_IDS_KEY);
    }

    public List<Long> findPublicPage(int start, int end) {
        Set<Object> members = redisTemplate.opsForZSet().reverseRange(PUBLIC_IDS_KEY, start, end);

        if (members == null || members.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> result = new ArrayList<>(members.size());
        for (Object member : members) {
            result.add(Long.valueOf(String.valueOf(member)));
        }
        return result;
    }

    public long countPublicIds() {
        Long size = redisTemplate.opsForZSet().zCard(PUBLIC_IDS_KEY);
        return size == null ? 0L : size;
    }

    public boolean existsFriendsKey() {
        return redisTemplate.hasKey(FRIENDS_IDS_KEY);
    }

    public List<Long> findFriendsPage(int start, int end) {
        Set<Object> members = redisTemplate.opsForZSet().reverseRange(FRIENDS_IDS_KEY, start, end);

        if (members == null || members.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> result = new ArrayList<>(members.size());
        for (Object member : members) {
            result.add(Long.valueOf(String.valueOf(member)));
        }
        return result;
    }

    public long countFriendsIds() {
        Long size = redisTemplate.opsForZSet().zCard(FRIENDS_IDS_KEY);
        return size == null ? 0L : size;
    }

    public boolean existsUserKey(Long userId) {
        return redisTemplate.hasKey(USER_IDS_KEY_PREFIX + userId);
    }

    public List<Long> findUserPage(Long userId, int start, int end) {
        Set<Object> members = redisTemplate.opsForZSet().reverseRange(USER_IDS_KEY_PREFIX + userId, start, end);

        if (members == null || members.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> result = new ArrayList<>(members.size());
        for (Object member : members) {
            result.add(Long.valueOf(String.valueOf(member)));
        }
        return result;
    }

    public long countUserIds(Long userId) {
        Long size = redisTemplate.opsForZSet().zCard(USER_IDS_KEY_PREFIX + userId);
        return size == null ? 0L : size;
    }
}