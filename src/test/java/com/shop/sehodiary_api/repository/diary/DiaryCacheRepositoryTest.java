package com.shop.sehodiary_api.repository.diary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.*;

import com.shop.sehodiary_api.web.dto.diary.DiaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class DiaryCacheRepositoryTest {

    private static final String DIARY_CACHE_KEY = "diary:cache:";

    @Mock
    private DiaryRepository diaryRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    private DiaryCacheRepository diaryCacheRepository;

    @BeforeEach
    void setUp() { diaryCacheRepository = new DiaryCacheRepository(diaryRepository, redisTemplate); }

    @Nested
    @DisplayName("get()")
    class GetTest {

        @Test
        @DisplayName("캐시에 값이 없으면 Optional.empty()를 반환한다")
        void returnsEmptyWhenCacheMiss() {
            Long diaryId = 1L;
            when(hashOperations.get(DIARY_CACHE_KEY, diaryId)).thenReturn(null);

            when(redisTemplate.opsForHash()).thenReturn(hashOperations);
            Optional<DiaryResponse> result = diaryCacheRepository.get(diaryId);

            assertThat(result).isEmpty();
            verify(hashOperations).get(DIARY_CACHE_KEY, diaryId);
        }

        @Test
        @DisplayName("캐시에 값이 있으면 Optional.of(DiaryResponse)를 반환한다")
        void returnsDiaryResponseWhenCacheHit() {
            Long diaryId = 1L;
            DiaryResponse response = mock(DiaryResponse.class);

            when(hashOperations.get(DIARY_CACHE_KEY, diaryId)).thenReturn(response);

            when(redisTemplate.opsForHash()).thenReturn(hashOperations);
            Optional<DiaryResponse> result = diaryCacheRepository.get(diaryId);

            assertThat(result).isPresent();
            assertThat(result.get()).isSameAs(response);
            verify(hashOperations).get(DIARY_CACHE_KEY, diaryId);
        }
    }

    @Test
    @DisplayName("put()은 Redis Hash에 DiaryResponse를 저장한다")
    void putStoresResponseInRedisHash() {
        DiaryResponse response = mock(DiaryResponse.class);
        when(response.getId()).thenReturn(1L);

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        diaryCacheRepository.put(response);

        verify(hashOperations).put(DIARY_CACHE_KEY, 1L, response);
    }

    @Test
    @DisplayName("delete()는 Redis Hash에서 diaryId로 삭제한다")
    void deleteRemovesDiaryFromRedisHash() {
        Long diaryId = 1L;

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        diaryCacheRepository.delete(diaryId);

        verify(hashOperations).delete(DIARY_CACHE_KEY, diaryId);
    }

    @Nested
    @DisplayName("getAll()")
    class GetAllTest {

        @Test
        @DisplayName("캐시에 값이 없으면 빈 Map을 반환한다")
        void returnsEmptyMapWhenNoEntries() {
            when(hashOperations.entries(DIARY_CACHE_KEY)).thenReturn(Collections.emptyMap());

            when(redisTemplate.opsForHash()).thenReturn(hashOperations);
            Map<Long, DiaryResponse> result = diaryCacheRepository.getAll();

            assertThat(result).isEmpty();
            verify(hashOperations).entries(DIARY_CACHE_KEY);
        }

        @Test
        @DisplayName("캐시에 값이 있으면 id를 key로 하는 Map을 반환한다")
        void returnsMappedEntries() {
            DiaryResponse response1 = mock(DiaryResponse.class);
            DiaryResponse response2 = mock(DiaryResponse.class);

            when(response1.getId()).thenReturn(1L);
            when(response2.getId()).thenReturn(2L);

            Map<Object, Object> entries = new HashMap<>();
            entries.put("1", response1);
            entries.put("2", response2);

            when(hashOperations.entries(DIARY_CACHE_KEY)).thenReturn(entries);

            when(redisTemplate.opsForHash()).thenReturn(hashOperations);
            Map<Long, DiaryResponse> result = diaryCacheRepository.getAll();

            assertThat(result).hasSize(2);
            assertThat(result.get(1L)).isSameAs(response1);
            assertThat(result.get(2L)).isSameAs(response2);
            verify(hashOperations).entries(DIARY_CACHE_KEY);
        }

        @Test
        @DisplayName("DiaryResponse의 id가 null이면 결과 Map에 포함하지 않는다")
        void ignoresEntryWhenDiaryResponseIdIsNull() {
            DiaryResponse validResponse = mock(DiaryResponse.class);
            DiaryResponse invalidResponse = mock(DiaryResponse.class);

            when(validResponse.getId()).thenReturn(1L);
            when(invalidResponse.getId()).thenReturn(null);

            Map<Object, Object> entries = new HashMap<>();
            entries.put("valid", validResponse);
            entries.put("invalid", invalidResponse);

            when(hashOperations.entries(DIARY_CACHE_KEY)).thenReturn(entries);

            when(redisTemplate.opsForHash()).thenReturn(hashOperations);
            Map<Long, DiaryResponse> result = diaryCacheRepository.getAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(1L)).isSameAs(validResponse);
        }
    }

    @Nested
    @DisplayName("evictDiaryCacheByUser()")
    class EvictDiaryCacheByUserTest {

        @Test
        @DisplayName("사용자의 diary id 목록이 있으면 해당 캐시들을 삭제한다")
        void deletesCacheEntriesWhenDiaryIdsExist() {
            Long userId = 10L;
            List<Long> diaryIds = List.of(1L, 2L, 3L);

            when(diaryRepository.findIdsByUserId(userId)).thenReturn(diaryIds);

            when(redisTemplate.opsForHash()).thenReturn(hashOperations);
            diaryCacheRepository.evictDiaryCacheByUser(userId);

            verify(diaryRepository).findIdsByUserId(userId);
            verify(hashOperations).delete(DIARY_CACHE_KEY, 1L, 2L, 3L);
        }

        @Test
        @DisplayName("사용자의 diary id 목록이 비어 있으면 삭제를 수행하지 않는다")
        void doesNothingWhenDiaryIdsAreEmpty() {
            Long userId = 10L;
            when(diaryRepository.findIdsByUserId(userId)).thenReturn(Collections.emptyList());

            diaryCacheRepository.evictDiaryCacheByUser(userId);

            verify(diaryRepository).findIdsByUserId(userId);
            verify(hashOperations, never()).delete(anyString(), any());
        }
    }

    @Nested
    class GetAllByIdsTest {
        @Test
        @DisplayName("ids가 null이면 빈 Map을 반환한다")
        void getAllByIds_nullIds_returnsEmptyMap() {
            Map<Long, DiaryResponse> result = diaryCacheRepository.getAllByIds(null);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("ids가 비어있으면 빈 Map을 반환한다")
        void getAllByIds_emptyIds_returnsEmptyMap() {
            Map<Long, DiaryResponse> result = diaryCacheRepository.getAllByIds(Collections.emptyList());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("redis에서 조회한 값들을 id 기준 Map으로 반환한다")
        void getAllByIds_returnsMappedResult() {
            // given
            List<Long> ids = List.of(1L, 2L, 3L);

            DiaryResponse diary1 = DiaryResponse.builder()
                    .id(1L)
                    .title("title1")
                    .content("content1")
                    .build();
            DiaryResponse diary3 = DiaryResponse.builder()
                    .id(3L)
                    .title("title3")
                    .content("content3")
                    .build();

            List<Object> redisValues = Arrays.asList(diary1, null, diary3);

            when(redisTemplate.opsForHash()).thenReturn(hashOperations);
            when(hashOperations.multiGet(
                    eq(DIARY_CACHE_KEY),
                    eq(List.of(1L, 2L, 3L))
            )).thenReturn(redisValues);

            // when
            Map<Long, DiaryResponse> result = diaryCacheRepository.getAllByIds(ids);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).containsEntry(1L, diary1);
            assertThat(result).containsEntry(3L, diary3);
            assertThat(result).doesNotContainKey(2L);

            verify(hashOperations).multiGet(eq(DIARY_CACHE_KEY), eq(List.of(1L, 2L, 3L)));
        }

        @Test
        @DisplayName("redis 조회값이 모두 null이면 빈 Map을 반환한다")
        void getAllByIds_allNullValues_returnsEmptyMap() {
            // given
            List<Long> ids = List.of(1L, 2L);

            when(redisTemplate.opsForHash()).thenReturn(hashOperations);
            when(hashOperations.multiGet(
                    eq(DIARY_CACHE_KEY),
                    eq(List.of(1L, 2L))
            )).thenReturn(Arrays.asList(null, null));

            // when
            Map<Long, DiaryResponse> result = diaryCacheRepository.getAllByIds(ids);

            // then
            assertThat(result).isEmpty();
        }
    }
}