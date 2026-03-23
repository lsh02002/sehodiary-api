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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiaryIdRedisRepositoryTest {

    private static final String PUBLIC_IDS_KEY = "diary:ids:public";
    private static final String FRIENDS_IDS_KEY = "diary:ids:friends";
    private static final String USER_IDS_KEY_PREFIX = "diary:ids:user:";

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private SetOperations<String, Object> setOperations;

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
            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            diaryIdRedisRepository.addPublic(1L);

            verify(setOperations).add(PUBLIC_IDS_KEY, 1L);
        }

        @Test
        @DisplayName("addFriends()는 친구공개 diary id를 저장한다")
        void addFriends() {
            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            diaryIdRedisRepository.addFriends(2L);

            verify(setOperations).add(FRIENDS_IDS_KEY, 2L);
        }

        @Test
        @DisplayName("addUser()는 사용자별 diary id를 저장한다")
        void addUser() {
            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            diaryIdRedisRepository.addUser(10L, 3L);

            verify(setOperations).add(USER_IDS_KEY_PREFIX + 10L, 3L);
        }
    }

    @Nested
    @DisplayName("remove 메서드")
    class RemoveTest {

        @Test
        @DisplayName("removePublic()는 공개 diary id를 삭제한다")
        void removePublic() {
            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            diaryIdRedisRepository.removePublic(1L);

            verify(setOperations).remove(PUBLIC_IDS_KEY, 1L);
        }

        @Test
        @DisplayName("removeFriends()는 친구공개 diary id를 삭제한다")
        void removeFriends() {
            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            diaryIdRedisRepository.removeFriends(2L);

            verify(setOperations).remove(FRIENDS_IDS_KEY, 2L);
        }

        @Test
        @DisplayName("removeUser()는 사용자별 diary id를 삭제한다")
        void removeUser() {
            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            diaryIdRedisRepository.removeUser(10L, 3L);

            verify(setOperations).remove(USER_IDS_KEY_PREFIX + 10L, 3L);
        }

        @Test
        @DisplayName("remove()는 public, friends set 양쪽에서 삭제한다")
        void remove() {
            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            diaryIdRedisRepository.remove(5L);

            verify(setOperations).remove(PUBLIC_IDS_KEY, 5L);
            verify(setOperations).remove(FRIENDS_IDS_KEY, 5L);
        }

        @Test
        @DisplayName("removeFromUser()는 사용자 set에서 diary id를 삭제한다")
        void removeFromUser() {
            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            diaryIdRedisRepository.removeFromUser(20L, 7L);

            verify(setOperations).remove(USER_IDS_KEY_PREFIX + 20L, 7L);
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

            when(setOperations.members(PUBLIC_IDS_KEY)).thenReturn(members);

            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            Set<Long> result = diaryIdRedisRepository.findAllPublic();

            assertThat(result).containsExactlyInAnyOrder(1L, 2L);
            verify(setOperations).members(PUBLIC_IDS_KEY);
        }

        @Test
        @DisplayName("findAllFriends()는 Long Set을 반환한다")
        void findAllFriends() {
            Set<Object> members = new LinkedHashSet<>();
            members.add("3");
            members.add(4L);

            when(setOperations.members(FRIENDS_IDS_KEY)).thenReturn(members);

            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            Set<Long> result = diaryIdRedisRepository.findAllFriends();

            assertThat(result).containsExactlyInAnyOrder(3L, 4L);
            verify(setOperations).members(FRIENDS_IDS_KEY);
        }

        @Test
        @DisplayName("findAllUser()는 사용자별 Long Set을 반환한다")
        void findAllUser() {
            Long userId = 100L;
            Set<Object> members = new LinkedHashSet<>();
            members.add("10");
            members.add(20L);

            when(setOperations.members(USER_IDS_KEY_PREFIX + userId)).thenReturn(members);

            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            Set<Long> result = diaryIdRedisRepository.findAllUser(userId);

            assertThat(result).containsExactlyInAnyOrder(10L, 20L);
            verify(setOperations).members(USER_IDS_KEY_PREFIX + userId);
        }

        @Test
        @DisplayName("members가 null이면 빈 Set을 반환한다")
        void returnsEmptySetWhenMembersIsNull() {
            when(setOperations.members(PUBLIC_IDS_KEY)).thenReturn(null);

            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            Set<Long> result = diaryIdRedisRepository.findAllPublic();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("members가 비어있으면 빈 Set을 반환한다")
        void returnsEmptySetWhenMembersIsEmpty() {
            when(setOperations.members(PUBLIC_IDS_KEY)).thenReturn(Collections.emptySet());

            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            Set<Long> result = diaryIdRedisRepository.findAllPublic();

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

            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            diaryIdRedisRepository.savePublicIds(ids);

            verify(setOperations).add(PUBLIC_IDS_KEY, 1L, 2L, 3L);
        }

        @Test
        @DisplayName("savePublicIds()는 ids가 null이면 저장하지 않는다")
        void savePublicIdsWithNull() {
            diaryIdRedisRepository.savePublicIds(null);

            verify(setOperations, never()).add(eq(PUBLIC_IDS_KEY), any());
        }

        @Test
        @DisplayName("savePublicIds()는 ids가 비어있으면 저장하지 않는다")
        void savePublicIdsWithEmptyList() {
            diaryIdRedisRepository.savePublicIds(Collections.emptyList());

            verify(setOperations, never()).add(eq(PUBLIC_IDS_KEY), any());
        }

        @Test
        @DisplayName("saveFriends()는 ids가 있으면 저장한다")
        void saveFriends() {
            List<Long> ids = List.of(4L, 5L);

            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            diaryIdRedisRepository.saveFriends(ids);

            verify(setOperations).add(FRIENDS_IDS_KEY, 4L, 5L);
        }

        @Test
        @DisplayName("saveFriends()는 ids가 null이면 저장하지 않는다")
        void saveFriendsWithNull() {
            diaryIdRedisRepository.saveFriends(null);

            verify(setOperations, never()).add(eq(FRIENDS_IDS_KEY), any());
        }

        @Test
        @DisplayName("saveFriends()는 ids가 비어있으면 저장하지 않는다")
        void saveFriendsWithEmptyList() {
            diaryIdRedisRepository.saveFriends(Collections.emptyList());

            verify(setOperations, never()).add(eq(FRIENDS_IDS_KEY), any());
        }

        @Test
        @DisplayName("saveUserIds()는 ids가 있으면 저장한다")
        void saveUserIds() {
            Long userId = 30L;
            List<Long> ids = List.of(7L, 8L);

            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            diaryIdRedisRepository.saveUserIds(userId, ids);

            verify(setOperations).add(USER_IDS_KEY_PREFIX + userId, 7L, 8L);
        }

        @Test
        @DisplayName("saveUserIds()는 ids가 null이면 저장하지 않는다")
        void saveUserIdsWithNull() {
            diaryIdRedisRepository.saveUserIds(30L, null);

            verify(setOperations, never()).add(startsWith(USER_IDS_KEY_PREFIX), any());
        }

        @Test
        @DisplayName("saveUserIds()는 ids가 비어있으면 저장하지 않는다")
        void saveUserIdsWithEmptyList() {
            diaryIdRedisRepository.saveUserIds(30L, Collections.emptyList());

            verify(setOperations, never()).add(startsWith(USER_IDS_KEY_PREFIX), any());
        }
    }
}