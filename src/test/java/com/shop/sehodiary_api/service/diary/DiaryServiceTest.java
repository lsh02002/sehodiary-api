package com.shop.sehodiary_api.service.diary;

import com.shop.sehodiary_api.repository.activity.ActivityAction;
import com.shop.sehodiary_api.repository.activity.ActivityEntityType;
import com.shop.sehodiary_api.repository.activity.function.SnapshotFunc;
import com.shop.sehodiary_api.repository.common.Visibility;
import com.shop.sehodiary_api.repository.diary.Diary;
import com.shop.sehodiary_api.repository.diary.DiaryCacheRepository;
import com.shop.sehodiary_api.repository.diary.DiaryIdRedisRepository;
import com.shop.sehodiary_api.repository.diary.DiaryRepository;
import com.shop.sehodiary_api.repository.like.LikeRepository;
import com.shop.sehodiary_api.repository.user.User;
import com.shop.sehodiary_api.repository.user.UserRepository;
import com.shop.sehodiary_api.repository.user.userDetails.CustomUserDetails;
import com.shop.sehodiary_api.service.activelog.ActivityLogService;
import com.shop.sehodiary_api.service.diaryemotion.DiaryEmotionService;
import com.shop.sehodiary_api.service.diaryimage.DiaryImageService;
import com.shop.sehodiary_api.service.diarysearch.DiarySearchIndexer;
import com.shop.sehodiary_api.service.exceptions.*;
import com.shop.sehodiary_api.service.webpush.WebPushService;
import com.shop.sehodiary_api.web.dto.diary.DiaryRequest;
import com.shop.sehodiary_api.web.dto.diary.DiaryResponse;
import com.shop.sehodiary_api.web.dto.fcm.PostCreatedEvent;
import com.shop.sehodiary_api.web.mapper.diary.DiaryMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class DiaryServiceTest {

    @InjectMocks
    private DiaryService diaryService;

    @Mock
    private DiarySearchIndexer diarySearchIndexer;

    private DiaryService spyService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DiaryRepository diaryRepository;

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private DiaryImageService diaryImageService;

    @Mock
    private DiaryEmotionService diaryEmotionService;

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private WebPushService webPushService;

    @Mock
    private DiaryMapper diaryMapper;

    @Mock
    private DiaryCacheRepository diaryCacheRepository;

    @Mock
    private DiaryIdRedisRepository diaryIdRedisRepository;

    @Mock
    private SnapshotFunc snapshotFunc;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private User user;
    private Diary diary;
    private DiaryRequest request;
    private DiaryResponse response;
    private List<MultipartFile> files;

    @BeforeEach
    void setUp() {
        spyService = spy(diaryService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("getDiariesByPublic()")
    class GetDiariesByPublicTest {

        @Test
        @DisplayName("Redis에 public ids가 있고 모두 cache에 있으며 visibility가 PUBLIC이면 cache만 반환한다")
        void returnsOnlyCachedPublicDiaries() {
            Long userId = 10L;
            Long id1 = 1L;
            Long id2 = 2L;

            Pageable pageable = PageRequest.of(0, 10);

            DiaryResponse response1 = mock(DiaryResponse.class);
            DiaryResponse response2 = mock(DiaryResponse.class);

            int start = (int) pageable.getOffset();
            int end = start + pageable.getPageSize() - 1;

            when(diaryIdRedisRepository.findPublicPage(start, end)).thenReturn(List.of(id1, id2));
            when(diaryCacheRepository.getAllByIds(List.of(id1, id2))).thenReturn(Map.of(
                    id1, response1,
                    id2, response2
            ));
            when(response1.getVisibility()).thenReturn(Visibility.PUBLIC.toString());
            when(response2.getVisibility()).thenReturn(Visibility.PUBLIC.toString());

            Page<DiaryResponse> result = diaryService.getDiariesByPublic(userId, pageable);

            assertThat(result).containsExactlyInAnyOrder(response1, response2);

            verify(diaryRepository).findAllPublicIds();
            verify(diaryRepository, never()).findAllById(anyList());
            verify(diaryCacheRepository, never()).put(any());
        }

        @Test
        @DisplayName("Redis에 public ids가 없으면 DB에서 ids를 조회하고 Redis에 저장한다")
        void loadsPublicIdsFromDbWhenRedisEmpty() {
            Long userId = 10L;
            Long id1 = 1L;
            Long id2 = 2L;

            Diary diary1 = mock(Diary.class);
            Diary diary2 = mock(Diary.class);

            Pageable pageable = PageRequest.of(0, 10);

            DiaryResponse response1 = mock(DiaryResponse.class);
            DiaryResponse response2 = mock(DiaryResponse.class);

            int start = (int) pageable.getOffset();
            int end = start + pageable.getPageSize() - 1;

            when(diaryIdRedisRepository.findPublicPage(start, end)).thenReturn(List.of(id1, id2));
            when(diaryRepository.findAllPublicIds()).thenReturn(List.of(id1, id2));
            when(diaryCacheRepository.getAllByIds(List.of(id1, id2))).thenReturn(new HashMap<>());

            when(diaryRepository.findAllById(anyIterable())).thenReturn(List.of(diary1, diary2));

            when(diary1.getVisibility()).thenReturn(Visibility.PUBLIC);
            when(diary2.getVisibility()).thenReturn(Visibility.PUBLIC);

            when(diaryMapper.toResponse(diary1)).thenReturn(response1);
            when(diaryMapper.toResponse(diary2)).thenReturn(response2);

            when(response1.getId()).thenReturn(id1);
            when(response2.getId()).thenReturn(id2);
            when(response1.getVisibility()).thenReturn(Visibility.PUBLIC.toString());
            when(response2.getVisibility()).thenReturn(Visibility.PUBLIC.toString());

            when(likeRepository.findLikedDiaryIds(eq(userId), anyList())).thenReturn(List.of());

            Page<DiaryResponse> result = diaryService.getDiariesByPublic(userId, pageable);

            assertThat(result).containsExactlyInAnyOrder(response1, response2);

            verify(diaryRepository).findAllPublicIds();
            verify(diaryIdRedisRepository).savePublicIds(List.of(id1, id2));
            verify(diaryRepository).findAllById(anyIterable());
            verify(diaryCacheRepository).put(response1);
            verify(diaryCacheRepository).put(response2);
        }

        @Test
        @DisplayName("cache에 있어도 visibility가 PUBLIC이 아니면 결과에서 제외한다")
        void excludesCachedDiaryWhenVisibilityIsNotPublic() {
            Long userId = 10L;
            Long id1 = 1L;
            Long id2 = 2L;

            Pageable pageable = PageRequest.of(0, 10);

            DiaryResponse publicResponse = mock(DiaryResponse.class);
            DiaryResponse friendsResponse = mock(DiaryResponse.class);

            int start = (int) pageable.getOffset();
            int end = start + pageable.getPageSize() - 1;

            when(diaryIdRedisRepository.findPublicPage(start, end)).thenReturn(List.of(id1, id2));
            when(diaryCacheRepository.getAllByIds(List.of(id1, id2))).thenReturn(Map.of(
                    id1, publicResponse,
                    id2, friendsResponse
            ));
            when(publicResponse.getVisibility()).thenReturn(Visibility.PUBLIC.toString());
            when(friendsResponse.getVisibility()).thenReturn(Visibility.FRIENDS.toString());

            Page<DiaryResponse> result = diaryService.getDiariesByPublic(userId, pageable);

            assertThat(result).containsExactly(publicResponse);

            verify(diaryRepository, never()).findAllById(anyList());
        }

        @Test
        @DisplayName("cache에 없는 diary만 DB에서 조회하고 PUBLIC diary만 반환 및 cache 저장한다")
        void loadsOnlyMissingPublicDiariesFromDb() {
            Long userId = 10L;
            Long id1 = 1L;
            Long id2 = 2L;
            Long id3 = 3L;

            Pageable pageable = PageRequest.of(0, 10);

            DiaryResponse cachedResponse = mock(DiaryResponse.class);
            DiaryResponse dbResponse = mock(DiaryResponse.class);

            Diary publicDiary = mock(Diary.class);
            Diary friendsDiary = mock(Diary.class);

            Map<Long, DiaryResponse> cacheMap = new HashMap<>();
            cacheMap.put(id1, cachedResponse);

            when(cachedResponse.getId()).thenReturn(id1);
            when(cachedResponse.getVisibility()).thenReturn(Visibility.PUBLIC.toString());
            when(dbResponse.getId()).thenReturn(id2);
            when(dbResponse.getVisibility()).thenReturn(Visibility.PUBLIC.toString());

            int start = (int) pageable.getOffset();
            int end = start + pageable.getPageSize() - 1;

            when(diaryIdRedisRepository.findPublicPage(start, end)).thenReturn(List.of(id1, id2, id3));
            when(diaryCacheRepository.getAllByIds(List.of(id1, id2, id3))).thenReturn(cacheMap);

            when(diaryRepository.findAllById(any())).thenReturn(List.of(publicDiary, friendsDiary));
            when(publicDiary.getVisibility()).thenReturn(Visibility.PUBLIC);
            when(friendsDiary.getVisibility()).thenReturn(Visibility.FRIENDS);
            when(diaryMapper.toResponse(publicDiary)).thenReturn(dbResponse);

            Page<DiaryResponse> result = diaryService.getDiariesByPublic(userId, pageable);

            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyInAnyOrder(cachedResponse, dbResponse);

            verify(diaryCacheRepository).put(dbResponse);
            verify(diaryMapper, never()).toResponse(friendsDiary);
        }
    }

    @Nested
    @DisplayName("getDiariesByFriends()")
    class GetDiariesByFriendsTest {

        @Test
        @DisplayName("Redis에 friends ids가 있고 모두 cache에 있으며 visibility가 FRIENDS면 cache만 반환한다")
        void returnsOnlyCachedFriendsDiaries() {
            Long userId = 10L;
            Long id1 = 1L;
            Long id2 = 2L;

            Pageable pageable = PageRequest.of(0, 10);

            DiaryResponse response1 = mock(DiaryResponse.class);
            DiaryResponse response2 = mock(DiaryResponse.class);

            int start = (int) pageable.getOffset();
            int end = start + pageable.getPageSize() - 1;

            when(diaryIdRedisRepository.findFriendsPage(start, end)).thenReturn(List.of(id1, id2));
            when(diaryCacheRepository.getAllByIds(List.of(id1, id2))).thenReturn(Map.of(
                    id1, response1,
                    id2, response2
            ));
            when(response1.getVisibility()).thenReturn(Visibility.FRIENDS.toString());
            when(response2.getVisibility()).thenReturn(Visibility.FRIENDS.toString());

            Page<DiaryResponse> result = diaryService.getDiariesByFriends(userId, pageable);

            assertThat(result).containsExactlyInAnyOrder(response1, response2);

            verify(diaryRepository).findAllFriendsIds();
            verify(diaryRepository, never()).findAllById(anyList());
            verify(diaryCacheRepository, never()).put(any());
        }

        @Test
        @DisplayName("Redis에 friends ids가 없으면 DB에서 ids를 조회하고 Redis에 저장한다")
        void loadsFriendsIdsFromDbWhenRedisEmpty() {
            Long userId = 10L;
            Long id1 = 1L;
            Long id2 = 2L;

            Pageable pageable = PageRequest.of(0, 10);

            Diary diary1 = mock(Diary.class);
            Diary diary2 = mock(Diary.class);

            DiaryResponse response1 = mock(DiaryResponse.class);
            DiaryResponse response2 = mock(DiaryResponse.class);

            int start = (int) pageable.getOffset();
            int end = start + pageable.getPageSize() - 1;

            when(diaryIdRedisRepository.findFriendsPage(start, end)).thenReturn(List.of(id1, id2));
            when(diaryRepository.findAllFriendsIds()).thenReturn(List.of(id1, id2));
            when(diaryCacheRepository.getAllByIds(List.of(id1, id2))).thenReturn(new HashMap<>());

            when(diaryRepository.findAllById(anyIterable())).thenReturn(List.of(diary1, diary2));

            when(diary1.getVisibility()).thenReturn(Visibility.FRIENDS);
            when(diary2.getVisibility()).thenReturn(Visibility.FRIENDS);

            when(diaryMapper.toResponse(diary1)).thenReturn(response1);
            when(diaryMapper.toResponse(diary2)).thenReturn(response2);

            when(response1.getId()).thenReturn(id1);
            when(response2.getId()).thenReturn(id2);
            when(response1.getVisibility()).thenReturn(Visibility.FRIENDS.toString());
            when(response2.getVisibility()).thenReturn(Visibility.FRIENDS.toString());

            when(likeRepository.findLikedDiaryIds(eq(userId), anyList())).thenReturn(List.of());

            Page<DiaryResponse> result = diaryService.getDiariesByFriends(userId, pageable);

            assertThat(result).containsExactlyInAnyOrder(response1, response2);

            verify(diaryRepository).findAllFriendsIds();
            verify(diaryIdRedisRepository).saveFriends(List.of(id1, id2));
            verify(diaryRepository).findAllById(anyIterable());
            verify(diaryCacheRepository).put(response1);
            verify(diaryCacheRepository).put(response2);
        }

        @Test
        @DisplayName("cache에 있어도 visibility가 FRIENDS가 아니면 결과에서 제외한다")
        void excludesCachedDiaryWhenVisibilityIsNotFriends() {
            Long userId = 10L;
            Long id1 = 1L;
            Long id2 = 2L;

            Pageable pageable = PageRequest.of(0, 10);

            DiaryResponse friendsResponse = mock(DiaryResponse.class);
            DiaryResponse publicResponse = mock(DiaryResponse.class);

            int start = (int) pageable.getOffset();
            int end = start + pageable.getPageSize() - 1;

            when(diaryIdRedisRepository.findFriendsPage(start, end)).thenReturn(List.of(id1, id2));
            when(diaryCacheRepository.getAllByIds(List.of(id1, id2))).thenReturn(Map.of(
                    id1, friendsResponse,
                    id2, publicResponse
            ));
            when(friendsResponse.getVisibility()).thenReturn(Visibility.FRIENDS.toString());
            when(publicResponse.getVisibility()).thenReturn(Visibility.PUBLIC.toString());

            Page<DiaryResponse> result = diaryService.getDiariesByFriends(userId, pageable);

            assertThat(result).containsExactly(friendsResponse);

            verify(diaryRepository, never()).findAllById(anyList());
        }

        @Test
        @DisplayName("cache에 없는 diary만 DB에서 조회하고 FRIENDS diary만 반환 및 cache 저장한다")
        void loadsOnlyMissingFriendsDiariesFromDb() {
            Long userId = 10L;
            Long id1 = 1L;
            Long id2 = 2L;
            Long id3 = 3L;

            Pageable pageable = PageRequest.of(0, 10);

            DiaryResponse cachedResponse = mock(DiaryResponse.class);
            DiaryResponse dbResponse = mock(DiaryResponse.class);

            Diary friendsDiary = mock(Diary.class);
            Diary publicDiary = mock(Diary.class);

            Map<Long, DiaryResponse> cachedMap = new HashMap<>();
            cachedMap.put(id1, cachedResponse);

            int start = (int) pageable.getOffset();
            int end = start + pageable.getPageSize() - 1;

            when(diaryIdRedisRepository.findFriendsPage(start, end)).thenReturn(List.of(id1, id2, id3));
            when(diaryCacheRepository.getAllByIds(List.of(id1, id2, id3))).thenReturn(cachedMap);

            when(cachedResponse.getId()).thenReturn(id1);
            when(cachedResponse.getVisibility()).thenReturn(Visibility.FRIENDS.toString());

            when(friendsDiary.getVisibility()).thenReturn(Visibility.FRIENDS);
            when(publicDiary.getVisibility()).thenReturn(Visibility.PUBLIC);

            when(diaryRepository.findAllById(anyIterable())).thenReturn(List.of(friendsDiary, publicDiary));
            when(diaryMapper.toResponse(friendsDiary)).thenReturn(dbResponse);

            when(dbResponse.getId()).thenReturn(id2);
            when(dbResponse.getVisibility()).thenReturn(Visibility.FRIENDS.toString());

            when(likeRepository.findLikedDiaryIds(eq(userId), anyList())).thenReturn(List.of());

            Page<DiaryResponse> result = diaryService.getDiariesByFriends(userId, pageable);

            assertThat(result).containsExactlyInAnyOrder(cachedResponse, dbResponse);

            verify(diaryRepository).findAllById(anyIterable());
            verify(diaryCacheRepository).put(dbResponse);
            verify(diaryMapper, never()).toResponse(publicDiary);
        }
    }

    @Nested
    @DisplayName("getDiariesByUser()")
    class GetDiariesByUserTest {

        @Test
        @DisplayName("Redis에 user diary ids가 있고 모두 cache에 있으면 cache만 반환한다")
        void returnsOnlyCachedUserDiaries() {
            Long userId = 10L;
            Long id1 = 1L;
            Long id2 = 2L;

            Pageable pageable = PageRequest.of(0, 10);

            DiaryResponse response1 = mock(DiaryResponse.class);
            DiaryResponse response2 = mock(DiaryResponse.class);

            int start = (int) pageable.getOffset();
            int end = start + pageable.getPageSize() - 1;

            when(diaryIdRedisRepository.findUserPage(userId, start, end)).thenReturn(List.of(id1, id2));
            when(diaryCacheRepository.getAllByIds(List.of(id1, id2))).thenReturn(Map.of(
                    id1, response1,
                    id2, response2
            ));

            Page<DiaryResponse> result = diaryService.getDiariesByUser(userId, userId, pageable);

            assertThat(result).containsExactlyInAnyOrder(response1, response2);

            verify(diaryRepository).findIdsByUserId(anyLong());
            verify(diaryRepository, never()).findAllById(anyList());
            verify(diaryCacheRepository, never()).put(any());
        }

        @Test
        @DisplayName("Redis에 user diary ids가 없으면 DB에서 ids 조회 후 Redis에 저장한다")
        void loadsUserIdsFromDbAndSavesRedis() {
            Long userId = 10L;
            Long id1 = 1L;
            Long id2 = 2L;

            Pageable pageable = PageRequest.of(0, 10);

            Diary diary1 = mock(Diary.class);
            Diary diary2 = mock(Diary.class);

            DiaryResponse response1 = mock(DiaryResponse.class);
            DiaryResponse response2 = mock(DiaryResponse.class);

            when(response1.getId()).thenReturn(id1);
            when(response2.getId()).thenReturn(id2);

            int start = (int) pageable.getOffset();
            int end = start + pageable.getPageSize() - 1;

            when(diaryIdRedisRepository.findUserPage(userId, start, end)).thenReturn(List.of(id1, id2));
            when(diaryRepository.findIdsByUserId(userId)).thenReturn(List.of(id1, id2));
            when(diaryCacheRepository.getAllByIds(List.of(id1, id2))).thenReturn(new HashMap<>());

            when(diaryRepository.findAllById(List.of(id1, id2))).thenReturn(List.of(diary1, diary2));
            when(diaryMapper.toResponse(diary1)).thenReturn(response1);
            when(diaryMapper.toResponse(diary2)).thenReturn(response2);
            when(likeRepository.findLikedDiaryIds(eq(userId), anyList())).thenReturn(List.of());

            Page<DiaryResponse> result = diaryService.getDiariesByUser(userId, userId, pageable);

            assertThat(result).containsExactlyInAnyOrder(response1, response2);

            verify(diaryRepository).findIdsByUserId(userId);
            verify(diaryIdRedisRepository).saveUserIds(userId, List.of(id1, id2));
            verify(diaryRepository).findAllById(List.of(id1, id2));
            verify(diaryCacheRepository).put(response1);
            verify(diaryCacheRepository).put(response2);
        }

        @Test
        @DisplayName("Redis가 비어 있고 DB에도 user diary ids가 없으면 빈 리스트를 반환한다")
        void returnsEmptyListWhenRedisAndDbAreEmpty() {
            Long userId = 10L;

            Pageable pageable = PageRequest.of(0, 10);

            int start = (int) pageable.getOffset();
            int end = start + pageable.getPageSize() - 1;

            when(diaryIdRedisRepository.findUserPage(userId, start, end)).thenReturn(List.of());
            when(diaryRepository.findIdsByUserId(userId)).thenReturn(List.of());

            Page<DiaryResponse> result = diaryService.getDiariesByUser(userId, userId, pageable);

            assertThat(result).isEmpty();

            verify(diaryRepository).findIdsByUserId(userId);
            verify(diaryIdRedisRepository, never()).saveUserIds(anyLong(), anyList());
            verify(diaryCacheRepository, never()).getAllByIds(anyList());
            verify(diaryRepository, never()).findAllById(anyList());
        }

        @Test
        @DisplayName("cache에 없는 diary만 DB에서 조회하고 cache에 저장 후 결과에 추가한다")
        void loadsOnlyMissingUserDiariesFromDb() {
            Long userId = 10L;
            Long id1 = 1L;
            Long id2 = 2L;
            Long id3 = 3L;

            Pageable pageable = PageRequest.of(0, 10);

            DiaryResponse cachedResponse = mock(DiaryResponse.class);
            DiaryResponse dbResponse1 = mock(DiaryResponse.class);
            DiaryResponse dbResponse2 = mock(DiaryResponse.class);

            Diary diary2 = mock(Diary.class);
            Diary diary3 = mock(Diary.class);

            Map<Long, DiaryResponse> cachedMap = new HashMap<>();
            cachedMap.put(id1, cachedResponse);

            given(cachedResponse.getId()).willReturn(id1);
            given(dbResponse1.getId()).willReturn(id2);
            given(dbResponse2.getId()).willReturn(id3);

            int start = (int) pageable.getOffset();
            int end = start + pageable.getPageSize() - 1;

            given(diaryIdRedisRepository.findUserPage(userId, start, end)).willReturn(List.of(id1, id2, id3));
            given(diaryCacheRepository.getAllByIds(List.of(id1, id2, id3))).willReturn(cachedMap);
            given(diaryRepository.findAllById(anyIterable())).willReturn(List.of(diary2, diary3));
            given(diaryMapper.toResponse(diary2)).willReturn(dbResponse1);
            given(diaryMapper.toResponse(diary3)).willReturn(dbResponse2);
            given(likeRepository.findLikedDiaryIds(eq(userId), anyList())).willReturn(List.of());

            Page<DiaryResponse> result = diaryService.getDiariesByUser(userId, userId, pageable);

            assertThat(result).containsExactlyInAnyOrder(cachedResponse, dbResponse1, dbResponse2);

            verify(diaryRepository).findAllById(anyIterable());
            verify(diaryCacheRepository).put(dbResponse1);
            verify(diaryCacheRepository).put(dbResponse2);
        }
    }

    @Nested
    @DisplayName("getDiariesPublicAndFriendsByUser")
    class getDiariesPublicAndFriendsByUser {
        @Test
        @DisplayName("Redis에 diaryIds가 없고 DB에도 없으면 빈 리스트 반환")
        void getDiariesPublicAndFriendsByUser_returnEmpty_whenNoDiaryIdsAnywhere() {
            // given
            Long userId = 1L;
            Long targetUserId = 2L;

            Pageable pageable = PageRequest.of(0, 10);

            int start = (int) pageable.getOffset();
            int end = start + pageable.getPageSize() - 1;

            given(diaryIdRedisRepository.findUserPage(targetUserId, start, end)).willReturn(List.of());
            given(diaryRepository.findIdsByUserId(targetUserId)).willReturn(List.of());

            // when
            Page<DiaryResponse> result = spyService.getDiariesPublicAndFriendsByUser(userId, targetUserId, pageable);

            // then
            assertThat(result).isEmpty();
            verify(diaryRepository).findIdsByUserId(targetUserId);
            verify(diaryIdRedisRepository, never()).saveUserIds(eq(targetUserId), any());
            verify(diaryCacheRepository, never()).getAllByIds(anyList());
        }

        @Test
        @DisplayName("Redis에 diaryIds가 없으면 DB 조회 후 Redis 저장, 캐시에서 모두 조회")
        void getDiariesPublicAndFriendsByUser_loadIdsFromDbAndSaveRedis() {
            // given
            Long userId = 1L;
            Long targetUserId = 2L;

            Pageable pageable = PageRequest.of(0, 10);

            List<Long> idsFromDb = List.of(10L, 20L);
            Map<Long, DiaryResponse> cachedMap = new HashMap<>();
            DiaryResponse diary1 = mockDiaryResponseWithId(10L);
            DiaryResponse diary2 = mockDiaryResponseWithId(20L);
            cachedMap.put(10L, diary1);
            cachedMap.put(20L, diary2);

            int start = (int) pageable.getOffset();
            int end = start + pageable.getPageSize() - 1;

            given(diaryIdRedisRepository.findUserPage(targetUserId, start, end)).willReturn(List.of(10L, 20L));
            given(diaryRepository.findIdsByUserId(targetUserId)).willReturn(idsFromDb);
            given(diaryCacheRepository.getAllByIds(idsFromDb)).willReturn(cachedMap);

            doReturn(true).when(spyService).isFriend(userId, targetUserId);
            doReturn(true).when(spyService).isVisibleToUser(any(DiaryResponse.class), eq(true));

            // when
            Page<DiaryResponse> result = spyService.getDiariesPublicAndFriendsByUser(userId, targetUserId, pageable);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyInAnyOrder(diary1, diary2);

            verify(diaryIdRedisRepository).saveUserIds(targetUserId, idsFromDb);
            verify(diaryCacheRepository).getAllByIds(idsFromDb);
            verify(diaryRepository, never()).findAllById(any());
        }

        @Test
        @DisplayName("캐시에 일부만 있으면 없는 diary만 DB 조회 후 캐시에 저장")
        void getDiariesPublicAndFriendsByUser_loadMissingFromDbAndPutCache() {
            // given
            Long userId = 1L;
            Long targetUserId = 2L;

            List<Long> diaryIds = List.of(10L, 20L, 30L);

            Pageable pageable = PageRequest.of(0, 10);

            DiaryResponse cachedDiary = mockDiaryResponseWithId(10L);
            DiaryResponse response20 = mockDiaryResponseWithId(20L);
            DiaryResponse response30 = mockDiaryResponseWithId(30L);

            Diary dbDiary20 = mock(Diary.class);
            Diary dbDiary30 = mock(Diary.class);

            Map<Long, DiaryResponse> cachedMap = new HashMap<>();
            cachedMap.put(10L, cachedDiary);

            int start = (int) pageable.getOffset();
            int end = start + pageable.getPageSize() - 1;

            given(diaryIdRedisRepository.findUserPage(targetUserId, start, end)).willReturn(diaryIds);
            given(diaryCacheRepository.getAllByIds(diaryIds)).willReturn(cachedMap);
            given(diaryRepository.findAllById(anyList())).willReturn(List.of(dbDiary20, dbDiary30));
            given(diaryMapper.toResponse(dbDiary20)).willReturn(response20);
            given(diaryMapper.toResponse(dbDiary30)).willReturn(response30);

            doReturn(false).when(spyService).isFriend(userId, targetUserId);
            doReturn(true).when(spyService).isVisibleToUser(cachedDiary, false);
            doReturn(true).when(spyService).isVisibleToUser(response20, false);
            doReturn(true).when(spyService).isVisibleToUser(response30, false);

            // when
            Page<DiaryResponse> result = spyService.getDiariesPublicAndFriendsByUser(userId, targetUserId, pageable);

            // then
            assertThat(result).hasSize(3);
            assertThat(result).containsExactlyInAnyOrder(cachedDiary, response20, response30);

            verify(diaryRepository).findAllById(
                    argThat((List<Long> ids) ->
                            ids.size() == 2 && ids.containsAll(List.of(20L, 30L)))
            );
            verify(diaryCacheRepository).put(response20);
            verify(diaryCacheRepository).put(response30);
        }

        @Test
        @DisplayName("친구가 아니면 공개 범위만 반환")
        void getDiariesPublicAndFriendsByUser_onlyPublicWhenNotFriend() {
            // given
            Long userId = 1L;
            Long targetUserId = 2L;

            List<Long> diaryIds = List.of(10L, 20L);

            Pageable pageable = PageRequest.of(0, 10);

            DiaryResponse publicDiary = mockDiaryResponse();
            DiaryResponse friendDiary = mockDiaryResponse();

            Map<Long, DiaryResponse> cachedMap = new HashMap<>();
            cachedMap.put(10L, publicDiary);
            cachedMap.put(20L, friendDiary);

            int start = (int) pageable.getOffset();
            int end = start + pageable.getPageSize() - 1;

            given(diaryIdRedisRepository.findUserPage(targetUserId, start, end)).willReturn(diaryIds);
            given(diaryCacheRepository.getAllByIds(diaryIds)).willReturn(cachedMap);
            given(likeRepository.findLikedDiaryIds(eq(userId), anyList())).willReturn(List.of());

            doReturn(false).when(spyService).isFriend(userId, targetUserId);
            doReturn(true).when(spyService).isVisibleToUser(publicDiary, false);
            doReturn(false).when(spyService).isVisibleToUser(friendDiary, false);

            // when
            Page<DiaryResponse> result = spyService.getDiariesPublicAndFriendsByUser(userId, targetUserId, pageable);

            // then
            assertThat(result).hasSize(1);
            assertThat(result).containsExactly(publicDiary);
            verify(diaryRepository, never()).findAllById(any());
        }

        @Test
        @DisplayName("친구면 공개 + 친구공개 범위 모두 반환")
        void getDiariesPublicAndFriendsByUser_publicAndFriendVisibleWhenFriend() {
            // given
            Long userId = 1L;
            Long targetUserId = 2L;

            List<Long> diaryIds = List.of(10L, 20L);

            Pageable pageable = PageRequest.of(0, 10);

            DiaryResponse publicDiary = mockDiaryResponseWithId(10L);
            DiaryResponse friendDiary = mockDiaryResponseWithId(20L);

            Map<Long, DiaryResponse> cachedMap = new HashMap<>();
            cachedMap.put(10L, publicDiary);
            cachedMap.put(20L, friendDiary);

            int start = (int) pageable.getOffset();
            int end = start + pageable.getPageSize() - 1;

            given(diaryIdRedisRepository.findUserPage(targetUserId, start, end)).willReturn(diaryIds);
            given(diaryCacheRepository.getAllByIds(diaryIds)).willReturn(cachedMap);

            doReturn(true).when(spyService).isFriend(userId, targetUserId);
            doReturn(true).when(spyService).isVisibleToUser(any(DiaryResponse.class), eq(true));

            // when
            Page<DiaryResponse> result = spyService.getDiariesPublicAndFriendsByUser(userId, targetUserId, pageable);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyInAnyOrder(publicDiary, friendDiary);
        }

        @Test
        @DisplayName("캐시와 DB 모두 조회했을 때 비공개 글은 제외")
        void getDiariesPublicAndFriendsByUser_excludeInvisibleDiaries() {
            // given
            Long userId = 1L;
            Long targetUserId = 2L;

            List<Long> diaryIds = List.of(10L, 20L, 30L);

            Pageable pageable = PageRequest.of(0, 10);

            DiaryResponse visibleCached = mockDiaryResponseWithId(10L);
            DiaryResponse visibleDb = mockDiaryResponseWithId(20L);
            DiaryResponse invisibleDb = mockDiaryResponseWithId(30L);

            Diary diary20 = mock(Diary.class);
            Diary diary30 = mock(Diary.class);

            Map<Long, DiaryResponse> cachedMap = new HashMap<>();
            cachedMap.put(10L, visibleCached);

            int start = (int) pageable.getOffset();
            int end = start + pageable.getPageSize() - 1;

            given(diaryIdRedisRepository.findUserPage(targetUserId, start, end)).willReturn(diaryIds);
            given(diaryCacheRepository.getAllByIds(diaryIds)).willReturn(cachedMap);
            given(diaryRepository.findAllById(any())).willReturn(List.of(diary20, diary30));
            given(diaryMapper.toResponse(diary20)).willReturn(visibleDb);
            given(diaryMapper.toResponse(diary30)).willReturn(invisibleDb);

            doReturn(false).when(spyService).isFriend(userId, targetUserId);
            doReturn(true).when(spyService).isVisibleToUser(visibleCached, false);
            doReturn(true).when(spyService).isVisibleToUser(visibleDb, false);
            doReturn(false).when(spyService).isVisibleToUser(invisibleDb, false);

            // when
            Page<DiaryResponse> result = spyService.getDiariesPublicAndFriendsByUser(userId, targetUserId, pageable);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyInAnyOrder(visibleCached, visibleDb);
            assertThat(result).doesNotContain(invisibleDb);
        }

        private DiaryResponse mockDiaryResponseWithId(Long id) {
            DiaryResponse response = mock(DiaryResponse.class);
            given(response.getId()).willReturn(id);
            return response;
        }

        private DiaryResponse mockDiaryResponse() {
            return mock(DiaryResponse.class);
        }
    }

    @Nested
    @DisplayName("getOneDiary()")
    class GetOneDiaryTest {

        @Test
        @DisplayName("cache에 있으면 cache에서 바로 조회한다")
        void returnsCachedDiary() {
            Long diaryId = 1L;
            String currentUserNickname = "user";

            DiaryResponse cachedResponse = mock(DiaryResponse.class);

            CustomUserDetails userDetails = mock(CustomUserDetails.class);
            Authentication authentication = mock(Authentication.class);
            SecurityContext securityContext = mock(SecurityContext.class);

            when(userDetails.getNickname()).thenReturn(currentUserNickname);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);

            when(diaryCacheRepository.get(diaryId)).thenReturn(Optional.of(cachedResponse));

            // 핵심: 캐시 응답에도 권한 체크용 값 세팅 필요
            when(cachedResponse.getVisibility()).thenReturn("PUBLIC");
            when(cachedResponse.getNickname()).thenReturn("user");

            DiaryResponse result = diaryService.getOneDiary(diaryId);

            assertThat(result).isSameAs(cachedResponse);

            verify(diaryRepository, never()).findById(any());
            verify(diaryMapper, never()).toResponse(any());
            verify(diaryCacheRepository, never()).put(any());
        }

        @Test
        @DisplayName("cache에 없으면 DB에서 조회 후 response로 변환하고 cache에 저장한다")
        void loadsDiaryFromDbAndCachesIt() {
            Long diaryId = 1L;
            String currentUserNickname = "user";

            Diary diary = mock(Diary.class);
            User user = mock(User.class);
            DiaryResponse response = mock(DiaryResponse.class);

            // 로그인 유저 세팅
            CustomUserDetails userDetails = mock(CustomUserDetails.class);
            Authentication authentication = mock(Authentication.class);
            SecurityContext securityContext = mock(SecurityContext.class);

            when(userDetails.getNickname()).thenReturn(currentUserNickname);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);

            // diary 조회 관련
            when(diaryCacheRepository.get(diaryId)).thenReturn(Optional.empty());
            when(diaryRepository.findById(diaryId)).thenReturn(Optional.of(diary));
            when(diaryMapper.toResponse(diary)).thenReturn(response);

            // 권한 체크 통과용
            when(diary.getVisibility()).thenReturn(Visibility.PUBLIC);
            when(diary.getUser()).thenReturn(user);
            when(user.getNickname()).thenReturn("user");

            DiaryResponse result = diaryService.getOneDiary(diaryId);

            assertThat(result).isSameAs(response);

            verify(diaryRepository).findById(diaryId);
            verify(diaryMapper).toResponse(diary);
            verify(diaryCacheRepository).put(response);
        }

        @Test
        @DisplayName("diary가 없으면 NotFoundException을 던진다")
        void throwsNotFoundExceptionWhenDiaryDoesNotExist() {
            Long diaryId = 1L;

            CustomUserDetails userDetails = mock(CustomUserDetails.class);
            Authentication authentication = mock(Authentication.class);
            SecurityContext securityContext = mock(SecurityContext.class);

            when(userDetails.getNickname()).thenReturn("loginUser");
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);

            when(diaryCacheRepository.get(diaryId)).thenReturn(Optional.empty());
            when(diaryRepository.findById(diaryId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> diaryService.getOneDiary(diaryId))
                    .isInstanceOf(NotFoundException.class)
                    .extracting("detailMessage")
                    .isEqualTo("해당 글을 찾을 수 없습니다.");
        }
    }

    @Nested
    class CreateDiaryTest {

        @BeforeEach
        void setUp() {
            user = User.builder()
                    .id(1L)
                    .build();

            request = new DiaryRequest();
            request.setTitle("오늘 일기");
            request.setContent("정말 즐거운 하루였다.");
            request.setDate(LocalDate.now().toString());
            request.setVisibility("PUBLIC");
            request.setWeather("SUNNY");
            request.setEmoji("😊");

            response = DiaryResponse.builder()
                    .id(100L)
                    .title("오늘 일기")
                    .content("정말 즐거운 하루였다.")
                    .build();

            files = List.of(
                    new MockMultipartFile(
                            "file",
                            "test.jpg",
                            "image/jpeg",
                            "dummy image".getBytes()
                    )
            );
        }

        @Test
        @DisplayName("일기 생성 성공 - PUBLIC 공개범위")
        void createDiary_success_public() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(snapshotFunc.snapshot(any(Diary.class))).willReturn(new HashMap<>());
            given(diaryMapper.toResponse(any(Diary.class))).willReturn(response);

            willAnswer(invocation -> {
                Diary diary = invocation.getArgument(0);
                // save 이후 id가 세팅된 상황 흉내
                org.springframework.test.util.ReflectionTestUtils.setField(diary, "id", 100L);
                return diary;
            }).given(diaryRepository).save(any(Diary.class));

            // when
            DiaryResponse result = diaryService.createDiary(1L, request, files);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getTitle()).isEqualTo("오늘 일기");

            verify(userRepository).findById(1L);
            verify(diaryRepository).save(any(Diary.class));
            verify(diaryImageService).uploadManyFiles(1L, 100L, files);
            verify(diaryEmotionService).createDiaryEmotion(1L, 100L, "😊");
            verify(activityLogService).log(
                    any(), any(), eq(100L), anyString(), eq(user), isNull(), any()
            );
            verify(diaryCacheRepository).put(response);

//            verify(webPushService).broadcast(
//                    anyString(),
//                    anyString(),
//                    anyString()
//            );
            verify(eventPublisher).publishEvent(any(PostCreatedEvent.class));
        }

        @Test
        @DisplayName("일기 생성 성공 - FRIENDS 공개범위")
        void createDiary_success_friends() {
            // given
            request.setVisibility("FRIENDS");

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(snapshotFunc.snapshot(any(Diary.class))).willReturn(new HashMap<>());
            given(diaryMapper.toResponse(any(Diary.class))).willReturn(response);

            willAnswer(invocation -> {
                Diary diary = invocation.getArgument(0);
                org.springframework.test.util.ReflectionTestUtils.setField(diary, "id", 200L);
                return diary;
            }).given(diaryRepository).save(any(Diary.class));

            // when
            diaryService.createDiary(1L, request, files);
        }

        @Test
        @DisplayName("사용자가 없으면 NotFoundException 발생")
        void createDiary_fail_userNotFound() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> diaryService.createDiary(1L, request, files))
                    .isInstanceOf(NotFoundException.class)
                    .extracting("detailMessage")
                    .isEqualTo("해당 사용자를 찾을 수 없습니다.");

            verify(diaryRepository, never()).save(any());
            verify(diaryImageService, never()).uploadManyFiles(anyLong(), anyLong(), anyList());
            verify(diaryEmotionService, never()).createDiaryEmotion(anyLong(), anyLong(), anyString());
        }

        @Nested
        @DisplayName("입력값 검증 실패")
        class ValidationFailTest {

            @Test
            @DisplayName("제목이 비어있으면 NotAcceptableException 발생")
            void createDiary_fail_emptyTitle() {
                // given
                request.setTitle("   ");
                given(userRepository.findById(1L)).willReturn(Optional.of(user));

                // when & then
                assertThatThrownBy(() -> diaryService.createDiary(1L, request, files))
                        .isInstanceOf(NotAcceptableException.class)
                        .extracting("detailMessage")
                        .isEqualTo("해당 제목란이 비어있습니다");

                verify(diaryRepository, never()).save(any());
            }

            @Test
            @DisplayName("내용이 비어있으면 NotAcceptableException 발생")
            void createDiary_fail_emptyContent() {
                // given
                request.setContent("   ");
                given(userRepository.findById(1L)).willReturn(Optional.of(user));

                // when & then
                assertThatThrownBy(() -> diaryService.createDiary(1L, request, files))
                        .isInstanceOf(NotAcceptableException.class)
                        .extracting("detailMessage")
                        .isEqualTo("해당 내용란이 비어있습니다");

                verify(diaryRepository, never()).save(any());
            }

            @Test
            @DisplayName("공개여부가 비어있으면 NotAcceptableException 발생")
            void createDiary_fail_emptyVisibility() {
                // given
                request.setVisibility("   ");
                given(userRepository.findById(1L)).willReturn(Optional.of(user));

                // when & then
                assertThatThrownBy(() -> diaryService.createDiary(1L, request, files))
                        .isInstanceOf(NotAcceptableException.class)
                        .extracting("detailMessage")
                        .isEqualTo("해당 공개여부란이 비어있습니다");

                verify(diaryRepository, never()).save(any());
            }

            @Test
            @DisplayName("날씨가 비어있으면 NotAcceptableException 발생")
            void createDiary_fail_emptyWeather() {
                // given
                request.setWeather("   ");
                given(userRepository.findById(1L)).willReturn(Optional.of(user));

                // when & then
                assertThatThrownBy(() -> diaryService.createDiary(1L, request, files))
                        .isInstanceOf(NotAcceptableException.class)
                        .extracting("detailMessage")
                        .isEqualTo("해당 날씨란이 비어있습니다");

                verify(diaryRepository, never()).save(any());
            }
        }
    }

    @Nested
    class EditDiaryTest {

        @BeforeEach
        void setUp() {
            user = User.builder()
                    .id(1L)
                    .build();

            diary = Diary.builder()
                    .user(user)
                    .title("기존 제목")
                    .content("기존 내용")
                    .date(LocalDate.now())
                    .visibility(Visibility.PRIVATE)
                    .weather("RAINY")
                    .build();

            org.springframework.test.util.ReflectionTestUtils.setField(diary, "id", 10L);

            request = new DiaryRequest();
            request.setTitle("수정된 제목");
            request.setContent("수정된 내용");
            request.setDate(LocalDate.now().toString());
            request.setVisibility("PUBLIC");
            request.setWeather("SUNNY");
            request.setEmoji("😊");

            response = DiaryResponse.builder()
                    .id(10L)
                    .title("수정된 제목")
                    .content("수정된 내용")
                    .build();

            files = List.of(
                    new MockMultipartFile(
                            "file",
                            "image.jpg",
                            "image/jpeg",
                            "dummy".getBytes()
                    )
            );
        }

        @Test
        @DisplayName("일기 수정 성공 - 모든 값 변경 + PUBLIC 반영")
        void editDiary_success_allChanged_public() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(diaryRepository.findById(10L)).willReturn(Optional.of(diary));
            given(diaryRepository.findByUserIdAndId(1L, 10L))
                    .willReturn(Optional.of(diary))
                    .willReturn(Optional.of(diary)); // flush 후 재조회
            given(snapshotFunc.snapshot(any(Diary.class)))
                    .willReturn(new HashMap<>())
                    .willReturn(new HashMap<>());
            given(diaryMapper.toResponse(any(Diary.class))).willReturn(response);

            // when
            DiaryResponse result = diaryService.editDiary(1L, 10L, request, files);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(10L);
            assertThat(diary.getTitle()).isEqualTo("수정된 제목");
            assertThat(diary.getContent()).isEqualTo("수정된 내용");
            assertThat(diary.getVisibility()).isEqualTo(Visibility.PUBLIC);
            assertThat(diary.getWeather()).isEqualTo("SUNNY");

            verify(userRepository).findById(1L);
            verify(diaryRepository, times(2)).findByUserIdAndId(1L, 10L);
            verify(diaryImageService).uploadManyFiles(1L, 10L, files);
            verify(diaryEmotionService).editDiaryEmotion(1L, 10L, "😊");
            verify(diaryRepository).flush();
            verify(activityLogService).log(
                    any(),
                    any(),
                    eq(10L),
                    anyString(),
                    eq(user),
                    eq(new HashMap<>()),
                    eq(new HashMap<>())
            );
            verify(diaryCacheRepository).put(response);
        }

        @Test
        @DisplayName("일기 수정 성공 - FRIENDS 반영")
        void editDiary_success_friends() {
            // given
            request.setVisibility("FRIENDS");

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(diaryRepository.findById(10L)).willReturn(Optional.of(diary));
            given(diaryRepository.findByUserIdAndId(1L, 10L))
                    .willReturn(Optional.of(diary))
                    .willReturn(Optional.of(diary));
            given(snapshotFunc.snapshot(any(Diary.class)))
                    .willReturn(new HashMap<>())
                    .willReturn(new HashMap<>());
            given(diaryMapper.toResponse(any(Diary.class))).willReturn(response);

            // when
            diaryService.editDiary(1L, 10L, request, files);

            // then
            verify(diaryIdRedisRepository, never()).addPublic(anyLong());
        }

        @Test
        @DisplayName("emoji가 null이면 감정 수정 호출 안 함")
        void editDiary_success_noEmoji() {
            // given
            request.setEmoji(null);

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(diaryRepository.findById(10L)).willReturn(Optional.of(diary));
            given(diaryRepository.findByUserIdAndId(1L, 10L))
                    .willReturn(Optional.of(diary))
                    .willReturn(Optional.of(diary));
            given(snapshotFunc.snapshot(any(Diary.class)))
                    .willReturn(new HashMap<>())
                    .willReturn(new HashMap<>());
            given(diaryMapper.toResponse(any(Diary.class))).willReturn(response);

            // when
            diaryService.editDiary(1L, 10L, request, files);

            // then
            verify(diaryEmotionService, never()).editDiaryEmotion(anyLong(), anyLong(), anyString());
            verify(diaryImageService).uploadManyFiles(1L, 10L, files);
            verify(diaryRepository).flush();
        }

        @Test
        @DisplayName("emoji가 공백이면 감정 수정 호출 안 함")
        void editDiary_success_blankEmoji() {
            // given
            request.setEmoji("   ");

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(diaryRepository.findById(10L)).willReturn(Optional.of(diary));
            given(diaryRepository.findByUserIdAndId(1L, 10L))
                    .willReturn(Optional.of(diary))
                    .willReturn(Optional.of(diary));
            given(snapshotFunc.snapshot(any(Diary.class)))
                    .willReturn(new HashMap<>())
                    .willReturn(new HashMap<>());
            given(diaryMapper.toResponse(any(Diary.class))).willReturn(response);

            // when
            diaryService.editDiary(1L, 10L, request, files);

            // then
            verify(diaryEmotionService, never()).editDiaryEmotion(anyLong(), anyLong(), anyString());
        }

        @Test
        @DisplayName("값이 동일해도 정상 동작하고 flush 및 부가 로직 수행")
        void editDiary_success_sameValues() {
            // given
            request.setTitle("기존 제목");
            request.setContent("기존 내용");
            request.setDate(LocalDate.now().toString());
            request.setVisibility("PRIVATE");
            request.setWeather("RAINY");
            request.setEmoji("🙂");

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(diaryRepository.findById(10L)).willReturn(Optional.of(diary));
            given(diaryRepository.findByUserIdAndId(1L, 10L))
                    .willReturn(Optional.of(diary))
                    .willReturn(Optional.of(diary));
            given(snapshotFunc.snapshot(any(Diary.class)))
                    .willReturn(new HashMap<>())
                    .willReturn(new HashMap<>());
            given(diaryMapper.toResponse(any(Diary.class))).willReturn(response);

            // when
            DiaryResponse result = diaryService.editDiary(1L, 10L, request, files);

            // then
            assertThat(result).isNotNull();
            assertThat(diary.getTitle()).isEqualTo("기존 제목");
            assertThat(diary.getContent()).isEqualTo("기존 내용");
            assertThat(diary.getVisibility()).isEqualTo(Visibility.PRIVATE);
            assertThat(diary.getWeather()).isEqualTo("RAINY");

            verify(diaryImageService).uploadManyFiles(1L, 10L, files);
            verify(diaryEmotionService).editDiaryEmotion(1L, 10L, "🙂");
            verify(diaryRepository).flush();
            verify(diaryCacheRepository).put(response);

            verify(diaryIdRedisRepository, never()).addPublic(anyLong());
            verify(diaryIdRedisRepository, never()).addFriends(anyLong());
        }
    }

    @Nested
    @DisplayName("예외 케이스")
    class ExceptionTest {

        @BeforeEach
        void setUp() {
            user = User.builder()
                    .id(1L)
                    .build();

            diary = Diary.builder()
                    .user(user)
                    .title("기존 제목")
                    .content("기존 내용")
                    .date(LocalDate.now())
                    .visibility(Visibility.PRIVATE)
                    .weather("RAINY")
                    .build();

            request = new DiaryRequest();
            request.setTitle("오늘 일기");
            request.setContent("정말 즐거운 하루였다.");
            request.setDate(LocalDate.now().toString());
            request.setVisibility("PUBLIC");
            request.setWeather("SUNNY");
            request.setEmoji("😊");
        }

        @Test
        @DisplayName("사용자가 없으면 NotFoundException")
        void editDiary_fail_userNotFound() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> diaryService.editDiary(1L, 10L, request, files))
                    .isInstanceOf(NotFoundException.class)
                    .extracting("detailMessage")
                    .isEqualTo("해당 사용자를 찾을 수 없습니다.");

            verify(diaryRepository, never()).findByUserIdAndId(anyLong(), anyLong());
            verify(diaryImageService, never()).uploadManyFiles(anyLong(), anyLong(), anyList());
            verify(diaryRepository, never()).flush();
        }

        @Test
        @DisplayName("해당 유저의 일기가 아니면 NotFoundException")
        void editDiary_fail_diaryNotFound() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(diaryRepository.findById(10L)).willReturn(Optional.of(diary));
            given(diaryRepository.findByUserIdAndId(1L, 10L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> diaryService.editDiary(1L, 10L, request, files))
                    .isInstanceOf(NotFoundException.class)
                    .extracting("detailMessage")
                    .isEqualTo("해당 사용자가 작성한 글이 아닙니다");

            verify(diaryImageService, never()).uploadManyFiles(anyLong(), anyLong(), anyList());
            verify(diaryEmotionService, never()).editDiaryEmotion(anyLong(), anyLong(), anyString());
            verify(diaryRepository, never()).flush();
        }

        @Test
        @DisplayName("flush 후 재조회에서 없으면 NotFoundException")
        void editDiary_fail_reloadedDiaryNotFound() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(diaryRepository.findById(10L)).willReturn(Optional.of(diary));
            given(diaryRepository.findByUserIdAndId(1L, 10L))
                    .willReturn(Optional.of(diary))
                    .willReturn(Optional.empty());
            given(snapshotFunc.snapshot(any(Diary.class))).willReturn(new HashMap<>());

            // when & then
            assertThatThrownBy(() -> diaryService.editDiary(1L, 10L, request, files))
                    .isInstanceOf(NotFoundException.class)
                    .extracting("detailMessage")
                    .isEqualTo("해당 사용자가 작성한 글이 아닙니다");

            verify(diaryImageService).uploadManyFiles(1L, 10L, files);
            verify(diaryRepository).flush();
            verify(activityLogService, never()).log(any(), any(), anyLong(), anyString(), any(), any(), any());
        }
    }

    @Nested
    class DeleteDiaryTest {
        private Long userId;
        private Long diaryId;
        private User user;
        private Diary diary;
        private Object beforeSnapshot;

        @BeforeEach
        void setup() {
            userId = 1L;
            diaryId = 10L;

            user = User.builder()
                    .id(userId)
                    .build();

            diary = Diary.builder()
                    .user(user)
                    .title("삭제할 일기")
                    .content("삭제할 내용")
                    .visibility(Visibility.PUBLIC)
                    .weather("SUNNY")
                    .build();

            ReflectionTestUtils.setField(diary, "id", diaryId);

            beforeSnapshot = new HashMap<>();
        }

        @Test
        @DisplayName("일기 삭제 성공")
        void deleteDiary_success() {
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(diaryRepository.findById(10L)).willReturn(Optional.of(diary));
            given(diaryRepository.findByUserIdAndId(userId, diaryId)).willReturn(Optional.of(diary));
            given(snapshotFunc.snapshot(diary)).willReturn(new HashMap<>());

            // when
            diaryService.deleteDiary(userId, diaryId);

            // then
            verify(userRepository).findById(userId);
            verify(diaryRepository).findByUserIdAndId(userId, diaryId);
            verify(diaryImageService).deleteManyFiles(diary);
            verify(snapshotFunc).snapshot(diary);
            verify(activityLogService).log(
                    eq(ActivityEntityType.DIARY),
                    eq(ActivityAction.DELETE),
                    eq(diaryId),
                    anyString(),
                    eq(user),
                    eq(beforeSnapshot),
                    isNull()
            );
            verify(diaryRepository).deleteByUserIdAndId(userId, diaryId);
            verify(diaryCacheRepository).delete(diaryId);
            verify(diaryIdRedisRepository).remove(diaryId);
            verify(diaryIdRedisRepository).removeFromUser(userId, diaryId);
            verify(diarySearchIndexer).delete(diaryId);
        }

        @Test
        @DisplayName("사용자가 없으면 ConflictException 발생")
        void deleteDiary_fail_userNotFound() {
            // given
            Long userId = 1L;
            Long diaryId = 10L;

            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> diaryService.deleteDiary(userId, diaryId))
                    .isInstanceOf(ConflictException.class)
                    .extracting("detailMessage")
                    .isEqualTo("해당 글을 삭제할 수 없습니다");

            verify(userRepository).findById(userId);
            verify(diaryRepository, never()).findByUserIdAndId(anyLong(), anyLong());
            verify(diaryImageService, never()).deleteManyFiles(any());
            verify(diaryRepository, never()).deleteByUserIdAndId(anyLong(), anyLong());
            verify(diaryCacheRepository, never()).delete(anyLong());
            verify(diaryIdRedisRepository, never()).remove(anyLong());
            verify(diaryIdRedisRepository, never()).removeFromUser(anyLong(), anyLong());
        }

        @Test
        @DisplayName("해당 유저의 일기가 아니면 ConflictException 발생")
        void deleteDiary_fail_diaryNotFound() {
            // given
            Long userId = 1L;
            Long diaryId = 10L;

            User user = mock(User.class);
            Diary diary = mock(Diary.class);

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(diaryRepository.findById(diaryId)).willReturn(Optional.of(diary));
            given(diaryRepository.findByUserIdAndId(userId, diaryId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> diaryService.deleteDiary(userId, diaryId))
                    .isInstanceOf(ConflictException.class)
                    .extracting("detailMessage")
                    .isEqualTo("해당 글을 삭제할 수 없습니다");

            verify(diaryImageService, never()).deleteManyFiles(any());
            verify(diaryRepository, never()).deleteByUserIdAndId(anyLong(), anyLong());
            verify(diaryCacheRepository, never()).delete(anyLong());
            verify(diaryIdRedisRepository, never()).remove(anyLong());
            verify(diaryIdRedisRepository, never()).removeFromUser(anyLong(), anyLong());
        }

        @Test
        @DisplayName("이미지 삭제 중 예외 발생 시 ConflictException 발생")
        void deleteDiary_fail_deleteFiles() {
            // given
            Long userId = 1L;
            Long diaryId = 10L;

            User user = mock(User.class);
            Diary diary = mock(Diary.class);

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(diaryRepository.findById(diaryId)).willReturn(Optional.of(diary));
            given(diaryRepository.findByUserIdAndId(userId, diaryId)).willReturn(Optional.of(diary));
            doThrow(new RuntimeException("S3 삭제 실패"))
                    .when(diaryImageService).deleteManyFiles(diary);

            // when & then
            assertThatThrownBy(() -> diaryService.deleteDiary(userId, diaryId))
                    .isInstanceOf(ConflictException.class)
                    .extracting("detailMessage")
                    .isEqualTo("해당 글을 삭제할 수 없습니다");

            verify(diaryImageService).deleteManyFiles(diary);
            verify(activityLogService, never()).log(any(), any(), anyLong(), anyString(), any(), any(), any());
            verify(diaryRepository, never()).deleteByUserIdAndId(anyLong(), anyLong());
            verify(diaryCacheRepository, never()).delete(anyLong());
            verify(diaryIdRedisRepository, never()).remove(anyLong());
            verify(diaryIdRedisRepository, never()).removeFromUser(anyLong(), anyLong());
        }

        @Test
        @DisplayName("DB 삭제 중 예외 발생 시 ConflictException 발생")
        void deleteDiary_fail_deleteRepository() {
            // given
            Long userId = 1L;
            Long diaryId = 10L;

            User user = mock(User.class);
            Diary diary = mock(Diary.class);

            Map<String, Object> beforeSnapshot = new HashMap<>();

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(diaryRepository.findById(diaryId)).willReturn(Optional.of(diary));
            given(diaryRepository.findByUserIdAndId(userId, diaryId)).willReturn(Optional.of(diary));

            given(diary.getId()).willReturn(diaryId);
            given(snapshotFunc.snapshot(diary)).willReturn(beforeSnapshot);

            doThrow(new RuntimeException("DB 삭제 실패"))
                    .when(diaryRepository).deleteByUserIdAndId(userId, diaryId);

            // when & then
            assertThatThrownBy(() -> diaryService.deleteDiary(userId, diaryId))
                    .isInstanceOf(ConflictException.class)
                    .extracting("detailMessage")
                    .isEqualTo("해당 글을 삭제할 수 없습니다");

            verify(diaryImageService).deleteManyFiles(diary);
            verify(activityLogService).log(
                    eq(ActivityEntityType.DIARY),
                    eq(ActivityAction.DELETE),
                    eq(diaryId),
                    nullable(String.class),
                    eq(user),
                    eq(beforeSnapshot),
                    isNull()
            );
            verify(diaryRepository).deleteByUserIdAndId(userId, diaryId);
            verify(diaryCacheRepository, never()).delete(anyLong());
            verify(diaryIdRedisRepository, never()).remove(anyLong());
            verify(diaryIdRedisRepository, never()).removeFromUser(anyLong(), anyLong());
        }
    }

    @Nested
    @DisplayName("validateDiaryAccess()")
    class ValidateDiaryAccessTest {

        @Test
        @DisplayName("PUBLIC 글이면 작성자가 아니어도 예외가 발생하지 않는다")
        void doesNotThrowWhenVisibilityIsPublic() {
            assertThatCode(() ->
                    ReflectionTestUtils.invokeMethod(
                            diaryService,
                            "validateDiaryAccess",
                            Visibility.PUBLIC,
                            "writerNickname",
                            "otherUserNickname"
                    )
            ).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("PRIVATE 글이고 작성자와 현재 사용자의 닉네임이 같으면 예외가 발생하지 않는다")
        void doesNotThrowWhenPrivateAndSameNickname() {
            assertThatCode(() ->
                    ReflectionTestUtils.invokeMethod(
                            diaryService,
                            "validateDiaryAccess",
                            Visibility.PRIVATE,
                            "writerNickname",
                            "writerNickname"
                    )
            ).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("PRIVATE 글이고 작성자와 현재 사용자의 닉네임이 다르면 AccessDeniedException이 발생한다")
        void throwsAccessDeniedExceptionWhenPrivateAndDifferentNickname() {
            assertThatThrownBy(() ->
                    ReflectionTestUtils.invokeMethod(
                            diaryService,
                            "validateDiaryAccess",
                            Visibility.PRIVATE,
                            "writerNickname",
                            "otherUserNickname"
                    )
            )
                    .isInstanceOf(AccessDeniedException.class)
                    .extracting("detailMessage")
                    .isEqualTo("비공개 글은 작성자만 조회할 수 있습니다.");
        }
    }

    @Nested
    @DisplayName("getCurrentUserNickname()")
    class GetCurrentUserNicknameTest {

        @Test
        @DisplayName("인증된 사용자의 principal이 CustomUserDetails면 nickname을 반환한다")
        void returnsNicknameWhenAuthenticatedUserExists() {
            String nickname = "seho";

            CustomUserDetails userDetails = mock(CustomUserDetails.class);
            Authentication authentication = mock(Authentication.class);
            SecurityContext securityContext = mock(SecurityContext.class);

            when(userDetails.getNickname()).thenReturn(nickname);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(securityContext.getAuthentication()).thenReturn(authentication);

            SecurityContextHolder.setContext(securityContext);

            String result = ReflectionTestUtils.invokeMethod(diaryService, "getCurrentUserNickname");

            assertThat(result).isEqualTo(nickname);
        }

        @Test
        @DisplayName("Authentication이 null이면 null이 리턴다")
        void throwsWhenAuthenticationIsNull() {
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(null);
            SecurityContextHolder.setContext(securityContext);

            String result = ReflectionTestUtils.invokeMethod(diaryService, "getCurrentUserNickname");

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("인증되지 않은 사용자면 null이 리턴된다")
        void throwsWhenNotAuthenticated() {
            Authentication authentication = mock(Authentication.class);
            SecurityContext securityContext = mock(SecurityContext.class);

            when(authentication.isAuthenticated()).thenReturn(false);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);

            String result = ReflectionTestUtils.invokeMethod(diaryService, "getCurrentUserNickname");

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("principal이 CustomUserDetails가 아니면 null을 반환한다")
        void returnsNullWhenPrincipalIsNotCustomUserDetails() {
            Authentication authentication = mock(Authentication.class);
            SecurityContext securityContext = mock(SecurityContext.class);

            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getPrincipal()).thenReturn("anonymousUser");
            when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);

            String result = ReflectionTestUtils.invokeMethod(diaryService, "getCurrentUserNickname");

            assertThat(result).isNull();
        }
    }
}
