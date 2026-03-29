package com.shop.sehodiary_api.repository.comment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentIdRedisRepositoryTest {

    @Mock
    private RedisTemplate<String, Long> redisTemplate;

    @Mock
    private ZSetOperations<String, Long> zSetOperations;

    private CommentIdRedisRepository commentIdRedisRepository;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        commentIdRedisRepository = new CommentIdRedisRepository(redisTemplate);
    }

    @Test
    @DisplayName("addByDiaryId - diaryId와 commentId가 있으면 리스트에 저장하고 TTL 설정")
    void addByDiaryId_success() {
        // given
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        // when
        commentIdRedisRepository.addByDiaryId(1L, 100L, 100.0);

        // then
        verify(zSetOperations).add("comments:diary:1", 100L, 100.0);
        verify(redisTemplate).expire("comments:diary:1", Duration.ofDays(1));
    }

    @Test
    @DisplayName("addByDiaryId - diaryId가 null이면 아무 작업도 하지 않음")
    void addByDiaryId_nullDiaryId() {
        // when
        commentIdRedisRepository.addByDiaryId(null, 1L, 0.0);

        verify(redisTemplate, never()).opsForZSet();
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("addByDiaryId - commentId가 null이면 아무 작업도 하지 않음")
    void addByDiaryId_nullCommentId() {
        // when
        commentIdRedisRepository.addByDiaryId(1L, null, 0.0);

        // then
        verify(redisTemplate, never()).opsForZSet();
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("addByUserId - userId와 commentId가 있으면 리스트에 저장하고 TTL 설정")
    void addByUserId_success() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        // when
        commentIdRedisRepository.addByUserId(2L, 200L, 0.0);

        // then
        verify(redisTemplate).expire("comments:user:2", Duration.ofDays(1));
    }

    @Test
    @DisplayName("addByUserId - userId가 null이면 아무 작업도 하지 않음")
    void addByUserId_nullUserId() {
        // when
        commentIdRedisRepository.addByUserId(null, 200L, 0.0);

        // then
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("addByUserId - commentId가 null이면 아무 작업도 하지 않음")
    void addByUserId_nullCommentId() {
        // when
        commentIdRedisRepository.addByUserId(2L, null, 0.0);

        // then
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("saveAllByDiaryId - 기존 키 삭제 후 전체 저장하고 TTL 설정")
    void saveAllByDiaryId_success() {
        // given
        List<Long> commentIds = List.of(1L, 2L, 3L);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        // when
        commentIdRedisRepository.saveAllByDiaryId(10L, commentIds);

        // then
        verify(redisTemplate).delete("comments:diary:10");
        verify(redisTemplate).expire("comments:diary:10", Duration.ofDays(1));
    }

    @Test
    @DisplayName("saveAllByDiaryId - diaryId가 null이면 아무 작업도 하지 않음")
    void saveAllByDiaryId_nullDiaryId() {
        // when
        commentIdRedisRepository.saveAllByDiaryId(null, List.of(1L, 2L));

        // then
        verify(redisTemplate, never()).delete(anyString());
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("saveAllByDiaryId - commentIds가 null이면 아무 작업도 하지 않음")
    void saveAllByDiaryId_nullCommentIds() {
        // when
        commentIdRedisRepository.saveAllByDiaryId(10L, null);

        // then
        verify(redisTemplate, never()).delete(anyString());
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("saveAllByDiaryId - commentIds가 비어있으면 아무 작업도 하지 않음")
    void saveAllByDiaryId_emptyCommentIds() {
        // when
        commentIdRedisRepository.saveAllByDiaryId(10L, Collections.emptyList());

        // then
        verify(redisTemplate, never()).delete(anyString());
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("saveAllByUserId - 기존 키 삭제 후 전체 저장하고 TTL 설정")
    void saveAllByUserId_success() {
        // given
        List<Long> commentIds = List.of(4L, 5L, 6L);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        // when
        commentIdRedisRepository.saveAllByUserId(20L, commentIds);

        // then
        verify(redisTemplate).delete("comments:user:20");
        verify(redisTemplate).expire("comments:user:20", Duration.ofDays(1));
    }

    @Test
    @DisplayName("findAllByDiaryId - diaryId가 null이면 빈 리스트 반환")
    void findAllByDiaryId_nullDiaryId() {
        // when
        List<Long> result = commentIdRedisRepository.findAllByDiaryId(null);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllByDiaryId - redis 값이 있으면 그대로 반환")
    void findAllByDiaryId_success() {
        Long diaryId = 1L;
        String key = "comments:diary:" + diaryId;

        Set<Long> ids = new LinkedHashSet<>(List.of(4L, 5L, 6L));

        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.range(key, 0, -1)).thenReturn(ids);

        List<Long> result = commentIdRedisRepository.findAllByDiaryId(diaryId);

        assertThat(result).containsExactly(4L, 5L, 6L);
    }

    @Test
    @DisplayName("findAllByDiaryId - redis 값이 null이면 빈 리스트 반환")
    void findAllByDiaryId_nullResult() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        // when
        List<Long> result = commentIdRedisRepository.findAllByDiaryId(10L);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllByUserId - userId가 null이면 빈 리스트 반환")
    void findAllByUserId_nullUserId() {
        // when
        List<Long> result = commentIdRedisRepository.findAllByUserId(null);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllByUserId - redis 값이 있으면 그대로 반환")
    void findAllByUserId_success() {
        long userId = 20L;
        String key = "comments:user:" + userId;

        Set<Long> ids = new LinkedHashSet<>(List.of(4L, 5L, 6L));

        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.range(key, 0, -1)).thenReturn(ids);

        // when
        List<Long> result = commentIdRedisRepository.findAllByUserId(20L);

        // then
        assertThat(result).containsExactly(4L, 5L, 6L);
    }

    @Test
    @DisplayName("findAllByUserId - redis 값이 null이면 빈 리스트 반환")
    void findAllByUserId_nullResult() {
        // given
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.range(eq("comments:user:20"), eq(0L), eq(-1L)))
                .thenReturn(null);

        // when
        List<Long> result = commentIdRedisRepository.findAllByUserId(20L);

        // then
        assertThat(result).isEmpty();
    }
    @Test
    @DisplayName("removeByDiaryId - diaryId와 commentId가 있으면 리스트에서 제거")
    void removeByDiaryId_success() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        // when
        commentIdRedisRepository.removeByDiaryId(10L, 100L);
    }

    @Test
    @DisplayName("removeByDiaryId - diaryId가 null이면 아무 작업도 하지 않음")
    void removeByDiaryId_nullDiaryId() {
        // when
        commentIdRedisRepository.removeByDiaryId(null, 100L);
    }

    @Test
    @DisplayName("removeByDiaryId - commentId가 null이면 아무 작업도 하지 않음")
    void removeByDiaryId_nullCommentId() {
        // when
        commentIdRedisRepository.removeByDiaryId(10L, null);
    }

    @Test
    @DisplayName("removeByUserId - userId와 commentId가 있으면 리스트에서 제거")
    void removeByUserId_success() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        // when
        commentIdRedisRepository.removeByUserId(20L, 200L);
    }

    @Test
    @DisplayName("removeByUserId - userId가 null이면 아무 작업도 하지 않음")
    void removeByUserId_nullUserId() {
        // when
        commentIdRedisRepository.removeByUserId(null, 200L);
    }

    @Test
    @DisplayName("removeByUserId - commentId가 null이면 아무 작업도 하지 않음")
    void removeByUserId_nullCommentId() {
        // when
        commentIdRedisRepository.removeByUserId(20L, null);
    }
}