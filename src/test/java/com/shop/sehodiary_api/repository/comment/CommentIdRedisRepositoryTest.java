package com.shop.sehodiary_api.repository.comment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CommentIdRedisRepositoryTest {

    private RedisTemplate<String, Long> redisTemplate;
    private ListOperations<String, Long> listOperations;

    private CommentIdRedisRepository commentIdRedisRepository;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = (RedisTemplate<String, Long>) mock(RedisTemplate.class);
        listOperations = (ListOperations<String, Long>) mock(ListOperations.class);

        when(redisTemplate.opsForList()).thenReturn(listOperations);

        commentIdRedisRepository = new CommentIdRedisRepository(redisTemplate);
    }

    @Test
    @DisplayName("addByDiaryId - diaryId와 commentId가 있으면 리스트에 저장하고 TTL 설정")
    void addByDiaryId_success() {
        // when
        commentIdRedisRepository.addByDiaryId(1L, 100L);

        // then
        verify(listOperations).rightPush("comments:diary:1", 100L);
        verify(redisTemplate).expire("comments:diary:1", Duration.ofDays(1));
    }

    @Test
    @DisplayName("addByDiaryId - diaryId가 null이면 아무 작업도 하지 않음")
    void addByDiaryId_nullDiaryId() {
        // when
        commentIdRedisRepository.addByDiaryId(null, 100L);

        // then
        verify(listOperations, never()).rightPush(anyString(), anyLong());
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("addByDiaryId - commentId가 null이면 아무 작업도 하지 않음")
    void addByDiaryId_nullCommentId() {
        // when
        commentIdRedisRepository.addByDiaryId(1L, null);

        // then
        verify(listOperations, never()).rightPush(anyString(), anyLong());
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("addByUserId - userId와 commentId가 있으면 리스트에 저장하고 TTL 설정")
    void addByUserId_success() {
        // when
        commentIdRedisRepository.addByUserId(2L, 200L);

        // then
        verify(listOperations).rightPush("comments:user:2", 200L);
        verify(redisTemplate).expire("comments:user:2", Duration.ofDays(1));
    }

    @Test
    @DisplayName("addByUserId - userId가 null이면 아무 작업도 하지 않음")
    void addByUserId_nullUserId() {
        // when
        commentIdRedisRepository.addByUserId(null, 200L);

        // then
        verify(listOperations, never()).rightPush(anyString(), anyLong());
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("addByUserId - commentId가 null이면 아무 작업도 하지 않음")
    void addByUserId_nullCommentId() {
        // when
        commentIdRedisRepository.addByUserId(2L, null);

        // then
        verify(listOperations, never()).rightPush(anyString(), anyLong());
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("saveAllByDiaryId - 기존 키 삭제 후 전체 저장하고 TTL 설정")
    void saveAllByDiaryId_success() {
        // given
        List<Long> commentIds = List.of(1L, 2L, 3L);

        // when
        commentIdRedisRepository.saveAllByDiaryId(10L, commentIds);

        // then
        verify(redisTemplate).delete("comments:diary:10");
        verify(listOperations).rightPushAll("comments:diary:10", commentIds);
        verify(redisTemplate).expire("comments:diary:10", Duration.ofDays(1));
    }

    @Test
    @DisplayName("saveAllByDiaryId - diaryId가 null이면 아무 작업도 하지 않음")
    void saveAllByDiaryId_nullDiaryId() {
        // when
        commentIdRedisRepository.saveAllByDiaryId(null, List.of(1L, 2L));

        // then
        verify(redisTemplate, never()).delete(anyString());
        verify(listOperations, never()).rightPushAll(anyString(), anyList());
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("saveAllByDiaryId - commentIds가 null이면 아무 작업도 하지 않음")
    void saveAllByDiaryId_nullCommentIds() {
        // when
        commentIdRedisRepository.saveAllByDiaryId(10L, null);

        // then
        verify(redisTemplate, never()).delete(anyString());
        verify(listOperations, never()).rightPushAll(anyString(), anyList());
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("saveAllByDiaryId - commentIds가 비어있으면 아무 작업도 하지 않음")
    void saveAllByDiaryId_emptyCommentIds() {
        // when
        commentIdRedisRepository.saveAllByDiaryId(10L, Collections.emptyList());

        // then
        verify(redisTemplate, never()).delete(anyString());
        verify(listOperations, never()).rightPushAll(anyString(), anyList());
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("saveAllByUserId - 기존 키 삭제 후 전체 저장하고 TTL 설정")
    void saveAllByUserId_success() {
        // given
        List<Long> commentIds = List.of(4L, 5L, 6L);

        // when
        commentIdRedisRepository.saveAllByUserId(20L, commentIds);

        // then
        verify(redisTemplate).delete("comments:user:20");
        verify(listOperations).rightPushAll("comments:user:20", commentIds);
        verify(redisTemplate).expire("comments:user:20", Duration.ofDays(1));
    }

    @Test
    @DisplayName("findAllByDiaryId - diaryId가 null이면 빈 리스트 반환")
    void findAllByDiaryId_nullDiaryId() {
        // when
        List<Long> result = commentIdRedisRepository.findAllByDiaryId(null);

        // then
        assertThat(result).isEmpty();
        verify(listOperations, never()).range(anyString(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("findAllByDiaryId - redis 값이 있으면 그대로 반환")
    void findAllByDiaryId_success() {
        // given
        List<Long> ids = List.of(1L, 2L, 3L);
        when(listOperations.range("comments:diary:10", 0, -1)).thenReturn(ids);

        // when
        List<Long> result = commentIdRedisRepository.findAllByDiaryId(10L);

        // then
        assertThat(result).containsExactly(1L, 2L, 3L);
    }

    @Test
    @DisplayName("findAllByDiaryId - redis 값이 null이면 빈 리스트 반환")
    void findAllByDiaryId_nullResult() {
        // given
        when(listOperations.range("comments:diary:10", 0, -1)).thenReturn(null);

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
        verify(listOperations, never()).range(anyString(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("findAllByUserId - redis 값이 있으면 그대로 반환")
    void findAllByUserId_success() {
        // given
        List<Long> ids = List.of(4L, 5L, 6L);
        when(listOperations.range("comments:user:20", 0, -1)).thenReturn(ids);

        // when
        List<Long> result = commentIdRedisRepository.findAllByUserId(20L);

        // then
        assertThat(result).containsExactly(4L, 5L, 6L);
    }

    @Test
    @DisplayName("findAllByUserId - redis 값이 null이면 빈 리스트 반환")
    void findAllByUserId_nullResult() {
        // given
        when(listOperations.range("comments:user:20", 0, -1)).thenReturn(null);

        // when
        List<Long> result = commentIdRedisRepository.findAllByUserId(20L);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("removeByDiaryId - diaryId와 commentId가 있으면 리스트에서 제거")
    void removeByDiaryId_success() {
        // when
        commentIdRedisRepository.removeByDiaryId(10L, 100L);

        // then
        verify(listOperations).remove("comments:diary:10", 1, 100L);
    }

    @Test
    @DisplayName("removeByDiaryId - diaryId가 null이면 아무 작업도 하지 않음")
    void removeByDiaryId_nullDiaryId() {
        // when
        commentIdRedisRepository.removeByDiaryId(null, 100L);

        // then
        verify(listOperations, never()).remove(anyString(), anyLong(), any());
    }

    @Test
    @DisplayName("removeByDiaryId - commentId가 null이면 아무 작업도 하지 않음")
    void removeByDiaryId_nullCommentId() {
        // when
        commentIdRedisRepository.removeByDiaryId(10L, null);

        // then
        verify(listOperations, never()).remove(anyString(), anyLong(), any());
    }

    @Test
    @DisplayName("removeByUserId - userId와 commentId가 있으면 리스트에서 제거")
    void removeByUserId_success() {
        // when
        commentIdRedisRepository.removeByUserId(20L, 200L);

        // then
        verify(listOperations).remove("comments:user:20", 1, 200L);
    }

    @Test
    @DisplayName("removeByUserId - userId가 null이면 아무 작업도 하지 않음")
    void removeByUserId_nullUserId() {
        // when
        commentIdRedisRepository.removeByUserId(null, 200L);

        // then
        verify(listOperations, never()).remove(anyString(), anyLong(), any());
    }

    @Test
    @DisplayName("removeByUserId - commentId가 null이면 아무 작업도 하지 않음")
    void removeByUserId_nullCommentId() {
        // when
        commentIdRedisRepository.removeByUserId(20L, null);

        // then
        verify(listOperations, never()).remove(anyString(), anyLong(), any());
    }
}