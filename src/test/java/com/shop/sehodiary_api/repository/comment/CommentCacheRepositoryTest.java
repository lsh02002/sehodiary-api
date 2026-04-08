package com.shop.sehodiary_api.repository.comment;

import com.shop.sehodiary_api.web.dto.comment.CommentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class CommentCacheRepositoryTest {

    @Mock
    private RedisTemplate<String, CommentResponse> redisTemplate;

    @Mock
    private ValueOperations<String, CommentResponse> valueOperations;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private CommentCacheRepository commentCacheRepository;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        commentRepository = mock(CommentRepository.class);

        commentCacheRepository = new CommentCacheRepository(redisTemplate, commentRepository);
    }

    @Test
    @DisplayName("put - commentId가 있으면 redis에 TTL과 함께 저장한다")
    void put_success() {
        // given
        CommentResponse response = mock(CommentResponse.class);
        when(response.getCommentId()).thenReturn(1L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // when
        commentCacheRepository.put(response);

        // then
        verify(valueOperations).set(
                "comment:1",
                response,
                Duration.ofDays(1)
        );
    }

    @Test
    @DisplayName("put - response가 null이면 저장하지 않는다")
    void put_nullResponse() {
        // when
        commentCacheRepository.put(null);

        // then
        verify(valueOperations, never()).set(anyString(), any(), any(Duration.class));
    }

    @Test
    @DisplayName("put - commentId가 null이면 저장하지 않는다")
    void put_nullCommentId() {
        // given
        CommentResponse response = mock(CommentResponse.class);
        when(response.getCommentId()).thenReturn(null);

        // when
        commentCacheRepository.put(response);

        // then
        verify(valueOperations, never()).set(anyString(), any(), any(Duration.class));
    }

    @Test
    @DisplayName("get - commentId가 null이면 empty 반환")
    void get_nullCommentId() {
        // when
        Optional<CommentResponse> result = commentCacheRepository.get(null);

        // then
        assertThat(result).isEmpty();
        verify(valueOperations, never()).get(anyString());
    }

    @Test
    @DisplayName("get - redis에 값이 있으면 Optional로 반환")
    void get_success() {
        // given
        CommentResponse response = mock(CommentResponse.class);
        when(valueOperations.get("comment:1")).thenReturn(response);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        // when
        Optional<CommentResponse> result = commentCacheRepository.get(1L);

        // then
        assertThat(result).contains(response);
    }

    @Test
    @DisplayName("get - redis에 값이 없으면 empty 반환")
    void get_notFound() {
        // given
        when(valueOperations.get("comment:1")).thenReturn(null);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // when
        Optional<CommentResponse> result = commentCacheRepository.get(1L);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("evict - commentId가 있으면 해당 key 삭제")
    void evict_success() {
        // when
        commentCacheRepository.evict(1L);

        // then
        verify(redisTemplate).delete("comment:1");
    }

    @Test
    @DisplayName("evict - commentId가 null이면 삭제하지 않는다")
    void evict_nullCommentId() {
        // when
        commentCacheRepository.evict(null);

        // then
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("getAll - key가 없으면 빈 map 반환")
    void getAll_empty() {
        // given
        when(redisTemplate.keys("comment:*")).thenReturn(Set.of());

        // when
        Map<Long, CommentResponse> result = commentCacheRepository.getAll();

        // then
        assertThat(result).isEmpty();
        verify(valueOperations, never()).multiGet(anyCollection());
    }

    @Test
    @DisplayName("getAll - 전체 캐시를 commentId 기준 map으로 반환")
    void getAll_success() {
        // given
        CommentResponse response1 = mock(CommentResponse.class);
        CommentResponse response2 = mock(CommentResponse.class);

        when(response1.getCommentId()).thenReturn(1L);
        when(response2.getCommentId()).thenReturn(2L);

        Set<String> keys = Set.of("comment:1", "comment:2");

        when(redisTemplate.keys("comment:*")).thenReturn(keys);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.multiGet(keys)).thenReturn(List.of(response1, response2));

        // when
        Map<Long, CommentResponse> result = commentCacheRepository.getAll();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(1L)).isEqualTo(response1);
        assertThat(result.get(2L)).isEqualTo(response2);
    }

    @Test
    @DisplayName("getAll - null 값은 map에 넣지 않는다")
    void getAll_skipNullValue() {
        // given
        CommentResponse response1 = mock(CommentResponse.class);
        when(response1.getCommentId()).thenReturn(1L);

        Set<String> keys = Set.of("comment:1", "comment:2");

        when(redisTemplate.keys("comment:*")).thenReturn(keys);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.multiGet(keys)).thenReturn(Arrays.asList(response1, null));

        // when
        Map<Long, CommentResponse> result = commentCacheRepository.getAll();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(1L)).isEqualTo(response1);
    }

    @Test
    @DisplayName("evictCommentCacheByUser - 유저의 댓글 캐시들을 삭제한다")
    void evictCommentCacheByUser_success() {
        // given
        when(commentRepository.findAllIdsByUserIdDesc(10L)).thenReturn(List.of(1L, 2L, 3L));

        // when
        commentCacheRepository.evictCommentCacheByUser(10L);

        // then
        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(redisTemplate).delete(captor.capture());

        assertThat(captor.getValue())
                .containsExactly("comment:1", "comment:2", "comment:3");
    }

    @Test
    @DisplayName("evictCommentCacheByUser - 댓글이 없으면 빈 리스트로 삭제 호출")
    void evictCommentCacheByUser_noComments() {
        // given
        when(commentRepository.findAllIdsByUserIdDesc(10L)).thenReturn(List.of());

        // when
        commentCacheRepository.evictCommentCacheByUser(10L);

        // then
        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(redisTemplate).delete(captor.capture());

        assertThat(captor.getValue()).isEmpty();
    }

    @Test
    void getAllByIds_정상조회() {
        // given
        List<Long> ids = List.of(1L, 2L, 3L);
        List<String> keys = List.of("comment:1", "comment:2", "comment:3");

        CommentResponse comment1 = mock(CommentResponse.class);
        CommentResponse comment3 = mock(CommentResponse.class);

        List<CommentResponse> values = Arrays.asList(
                comment1,
                null,
                comment3
        );

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.multiGet(keys)).thenReturn(values);

        // when
        Map<Long, CommentResponse> result = commentCacheRepository.getAllByIds(ids);

        // then
        assertEquals(2, result.size());
        assertEquals(comment1, result.get(1L));
        assertEquals(comment3, result.get(3L));
        assertFalse(result.containsKey(2L));

        verify(redisTemplate).opsForValue();
        verify(valueOperations).multiGet(keys);
    }

    @Test
    void getAllByIds_빈리스트면_빈맵반환() {
        // given
        List<Long> ids = List.of();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.multiGet(List.of())).thenReturn(List.of());

        // when
        Map<Long, CommentResponse> result = commentCacheRepository.getAllByIds(ids);

        // then
        assertTrue(result.isEmpty());

        verify(redisTemplate).opsForValue();
        verify(valueOperations).multiGet(List.of());
    }

    @Test
    void getAllByIds_모두null이면_빈맵반환() {
        // given
        List<Long> ids = List.of(10L, 20L);
        List<String> keys = List.of("comment:10", "comment:20");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.multiGet(keys)).thenReturn(Arrays.asList(null, null));

        // when
        Map<Long, CommentResponse> result = commentCacheRepository.getAllByIds(ids);

        // then
        assertTrue(result.isEmpty());

        verify(redisTemplate).opsForValue();
        verify(valueOperations).multiGet(keys);
    }
}