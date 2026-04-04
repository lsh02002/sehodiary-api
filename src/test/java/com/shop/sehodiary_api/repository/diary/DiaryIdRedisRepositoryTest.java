package com.shop.sehodiary_api.repository.diary;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class DiaryIdRedisRepositoryTest {

    private static final String PUBLIC_IDS_KEY = "diary:ids:public";
    private static final String FRIENDS_IDS_KEY = "diary:ids:friends";
    private static final String USER_IDS_KEY_PREFIX = "diary:ids:user:";

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    private DiaryIdRedisRepository diaryIdRedisRepository;

    @BeforeEach
    void setUp() {
        diaryIdRedisRepository = new DiaryIdRedisRepository(redisTemplate);
    }

    @Nested
    @DisplayName("add 메서드")
    class AddTest {

        @Test
        @DisplayName("addPublic()는 공개 diary id를 저장한다")
        void addPublic() {
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            diaryIdRedisRepository.addPublic(1L);

            verify(zSetOperations).add(PUBLIC_IDS_KEY, 1L, 1.0);
        }

        @Test
        @DisplayName("addFriends()는 친구공개 diary id를 저장한다")
        void addFriends() {
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            diaryIdRedisRepository.addFriends(2L);

            verify(zSetOperations).add(FRIENDS_IDS_KEY, 2L, 2.0);
        }

        @Test
        @DisplayName("addUser()는 사용자별 diary id를 저장한다")
        void addUser() {
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            diaryIdRedisRepository.addUser(10L, 3L);

            verify(zSetOperations).add(USER_IDS_KEY_PREFIX + 10L, 3L, 3.0);
        }
    }

    @Nested
    @DisplayName("remove 메서드")
    class RemoveTest {

        @Test
        @DisplayName("removePublic()는 공개 diary id를 삭제한다")
        void removePublic() {
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            diaryIdRedisRepository.removePublic(1L);

            verify(zSetOperations).remove(PUBLIC_IDS_KEY, 1L);
        }

        @Test
        @DisplayName("removeFriends()는 친구공개 diary id를 삭제한다")
        void removeFriends() {
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            diaryIdRedisRepository.removeFriends(2L);

            verify(zSetOperations).remove(FRIENDS_IDS_KEY, 2L);
        }

        @Test
        @DisplayName("removeUser()는 사용자별 diary id를 삭제한다")
        void removeUser() {
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            diaryIdRedisRepository.removeUser(10L, 3L);

            verify(zSetOperations).remove(USER_IDS_KEY_PREFIX + 10L, 3L);
        }

        @Test
        @DisplayName("remove()는 public, friends set 양쪽에서 삭제한다")
        void remove() {
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            diaryIdRedisRepository.remove(5L);

            verify(zSetOperations).remove(PUBLIC_IDS_KEY, 5L);
            verify(zSetOperations).remove(FRIENDS_IDS_KEY, 5L);
        }

        @Test
        @DisplayName("removeFromUser()는 사용자 set에서 diary id를 삭제한다")
        void removeFromUser() {
            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            diaryIdRedisRepository.removeFromUser(20L, 7L);

            verify(zSetOperations).remove(USER_IDS_KEY_PREFIX + 20L, 7L);
        }
    }

    @Nested
    @DisplayName("findAll 메서드")
    class FindAllTest {

        @Test
        @DisplayName("findAllPublic()는 Long Set을 반환한다")
        void findAllPublic() {
            Set<Object> members = new LinkedHashSet<>();
            members.add(1L);
            members.add("2");

            when(zSetOperations.reverseRange(PUBLIC_IDS_KEY, 0, -1)).thenReturn(members);

            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            List<Long> result = diaryIdRedisRepository.findAllPublic();

            assertThat(result).containsExactlyInAnyOrder(1L, 2L);
            verify(zSetOperations).reverseRange(PUBLIC_IDS_KEY, 0, -1);
        }

        @Test
        @DisplayName("findAllFriends()는 Long Set을 반환한다")
        void findAllFriends() {
            Set<Object> members = new LinkedHashSet<>();
            members.add("3");
            members.add(4L);

            when(zSetOperations.reverseRange(FRIENDS_IDS_KEY, 0, -1)).thenReturn(members);

            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            List<Long> result = diaryIdRedisRepository.findAllFriends();

            assertThat(result).containsExactlyInAnyOrder(3L, 4L);
            verify(zSetOperations).reverseRange(FRIENDS_IDS_KEY, 0, -1);
        }

        @Test
        @DisplayName("findAllUser()는 사용자별 Long Set을 반환한다")
        void findAllUser() {
            Long userId = 100L;
            Set<Object> members = new LinkedHashSet<>();
            members.add("10");
            members.add(20L);

            when(zSetOperations.reverseRange(USER_IDS_KEY_PREFIX + userId, 0, -1)).thenReturn(members);

            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            List<Long> result = diaryIdRedisRepository.findAllUser(userId);

            assertThat(result).containsExactlyInAnyOrder(10L, 20L);
            verify(zSetOperations).reverseRange(USER_IDS_KEY_PREFIX + userId, 0, -1);
        }

        @Test
        @DisplayName("members가 null이면 빈 Set을 반환한다")
        void returnsEmptySetWhenMembersIsNull() {
            when(zSetOperations.reverseRange(PUBLIC_IDS_KEY,0, -1)).thenReturn(null);

            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            List<Long> result = diaryIdRedisRepository.findAllPublic();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("members가 비어있으면 빈 Set을 반환한다")
        void returnsEmptySetWhenMembersIsEmpty() {
            when(zSetOperations.reverseRange(PUBLIC_IDS_KEY, 0, -1)).thenReturn(Collections.emptySet());

            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            List<Long> result = diaryIdRedisRepository.findAllPublic();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("save 메서드")
    class SaveTest {

        @Test
        @DisplayName("savePublicIds()는 ids가 있으면 저장한다")
        void savePublicIds() {
            List<Long> ids = List.of(1L, 2L, 3L);

            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            diaryIdRedisRepository.savePublicIds(ids);

            verify(zSetOperations).add(eq(PUBLIC_IDS_KEY), anySet());
        }

        @Test
        @DisplayName("savePublicIds()는 ids가 null이면 저장하지 않는다")
        void savePublicIdsWithNull() {
            diaryIdRedisRepository.savePublicIds(null);

            verify(zSetOperations, never()).add(eq(PUBLIC_IDS_KEY), any());
        }

        @Test
        @DisplayName("savePublicIds()는 ids가 비어있으면 저장하지 않는다")
        void savePublicIdsWithEmptyList() {
            diaryIdRedisRepository.savePublicIds(Collections.emptyList());

            verify(zSetOperations, never()).add(eq(PUBLIC_IDS_KEY), any());
        }

        @Test
        @DisplayName("saveFriends()는 ids가 있으면 저장한다")
        void saveFriends() {
            List<Long> ids = List.of(4L, 5L);

            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

            diaryIdRedisRepository.saveFriends(ids);

            verify(zSetOperations).add(eq(FRIENDS_IDS_KEY), anySet());
        }

        @Test
        @DisplayName("saveFriends()는 ids가 null이면 저장하지 않는다")
        void saveFriendsWithNull() {
            diaryIdRedisRepository.saveFriends(null);

            verify(zSetOperations, never()).add(eq(FRIENDS_IDS_KEY), any());
        }

        @Test
        @DisplayName("saveFriends()는 ids가 비어있으면 저장하지 않는다")
        void saveFriendsWithEmptyList() {
            diaryIdRedisRepository.saveFriends(Collections.emptyList());

            verify(zSetOperations, never()).add(eq(FRIENDS_IDS_KEY), any());
        }

        @Test
        @DisplayName("saveUserIds()는 ids가 있으면 저장한다")
        void saveUserIds() {
            Long userId = 30L;
            List<Long> ids = List.of(7L, 8L);

            when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
            diaryIdRedisRepository.saveUserIds(userId, ids);

            verify(zSetOperations).add(eq(USER_IDS_KEY_PREFIX + userId), anySet());
        }

        @Test
        @DisplayName("saveUserIds()는 ids가 null이면 저장하지 않는다")
        void saveUserIdsWithNull() {
            diaryIdRedisRepository.saveUserIds(30L, null);

            verify(zSetOperations, never()).add(startsWith(USER_IDS_KEY_PREFIX), any());
        }

        @Test
        @DisplayName("saveUserIds()는 ids가 비어있으면 저장하지 않는다")
        void saveUserIdsWithEmptyList() {
            diaryIdRedisRepository.saveUserIds(30L, Collections.emptyList());

            verify(zSetOperations, never()).add(startsWith(USER_IDS_KEY_PREFIX), any());
        }
    }
}