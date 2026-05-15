package com.shop.sehodiary_api.service.diarysearch;

import com.shop.sehodiary_api.repository.common.Visibility;
import com.shop.sehodiary_api.repository.diary.DiaryCacheRepository;
import com.shop.sehodiary_api.repository.diary.DiaryRepository;
import com.shop.sehodiary_api.config.redis.diarysearch.DiarySearchDocument;
import com.shop.sehodiary_api.config.redis.diarysearch.DiarySearchRepository;
import com.shop.sehodiary_api.repository.follow.FollowRepository;
import com.shop.sehodiary_api.repository.like.LikeRepository;
import com.shop.sehodiary_api.web.dto.diary.DiaryResponse;
import com.shop.sehodiary_api.web.mapper.diary.DiaryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiarySearchService {

    private final DiarySearchRepository diarySearchRepository;
    private final DiaryCacheRepository diaryCacheRepository;
    private final DiaryRepository diaryRepository;
    private final DiaryMapper diaryMapper;
    private final LikeRepository likeRepository;
    private final FollowRepository followRepository;

    public Page<DiaryResponse> searchPublicDiaries(
            Long userId,
            String keyword,
            Pageable pageable
    ) {
        String q = keyword == null ? "" : keyword.trim();

        if (q.isBlank()) {
            return Page.empty(pageable);
        }

        Page<DiarySearchDocument> docPage =
                diarySearchRepository
                        .findByVisibilityAndTitleContainingOrVisibilityAndContentContaining(
                                Visibility.PUBLIC.name(),
                                q,
                                Visibility.PUBLIC.name(),
                                q,
                                pageable
                        );

        List<Long> diaryIds = docPage.getContent().stream()
                .map(DiarySearchDocument::getDiaryId)
                .toList();

        if (diaryIds.isEmpty()) {
            return Page.empty(pageable);
        }

        Map<Long, DiaryResponse> cached =
                diaryCacheRepository.getAllByIds(diaryIds);

        List<Long> missingIds = diaryIds.stream()
                .filter(id -> !cached.containsKey(id))
                .toList();

        if (!missingIds.isEmpty()) {
            List<DiaryResponse> dbResults =
                    diaryRepository.findAllById(missingIds).stream()
                            .filter(diary -> diary.getVisibility() == Visibility.PUBLIC)
                            .map(diaryMapper::toResponse)
                            .toList();

            dbResults.forEach(response -> {
                diaryCacheRepository.put(response);
                cached.put(response.getId(), response);
            });
        }

        List<DiaryResponse> result = diaryIds.stream()
                .map(cached::get)
                .filter(Objects::nonNull)
                .filter(response ->
                        Visibility.PUBLIC.name().equals(response.getVisibility())
                )
                .toList();

        applyLiked(userId, result);

        return new PageImpl<>(result, pageable, docPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<DiaryResponse> searchDiariesPublicAndFriendsByUser(
            Long userId,
            Long targetUserId,
            String keyword,
            Pageable pageable
    ) {
        boolean isOwner = Objects.equals(userId, targetUserId);
        boolean isFriend = isFriend(userId, targetUserId);

        Page<DiarySearchDocument> docPage;

        if (isOwner) {
            docPage = diarySearchRepository
                    .findByUserIdAndTitleContainingOrUserIdAndContentContaining(
                            targetUserId,
                            keyword,
                            targetUserId,
                            keyword,
                            pageable
                    );
        } else if (isFriend) {
            docPage = diarySearchRepository
                    .findByUserIdAndVisibilityInAndTitleContainingOrUserIdAndVisibilityInAndContentContaining(
                            targetUserId,
                            List.of(Visibility.PUBLIC.name(), Visibility.FRIENDS.name()),
                            keyword,
                            targetUserId,
                            List.of(Visibility.PUBLIC.name(), Visibility.FRIENDS.name()),
                            keyword,
                            pageable
                    );
        } else {
            docPage = diarySearchRepository
                    .findByUserIdAndVisibilityAndTitleContainingOrUserIdAndVisibilityAndContentContaining(
                            targetUserId,
                            Visibility.PUBLIC.name(),
                            keyword,
                            targetUserId,
                            Visibility.PUBLIC.name(),
                            keyword,
                            pageable
                    );
        }

        List<Long> diaryIds = docPage.getContent().stream()
                .map(DiarySearchDocument::getDiaryId)
                .toList();

        if (diaryIds.isEmpty()) {
            return Page.empty(pageable);
        }

        Map<Long, DiaryResponse> cached =
                diaryCacheRepository.getAllByIds(diaryIds);

        List<Long> missingIds = diaryIds.stream()
                .filter(id -> !cached.containsKey(id))
                .toList();

        if (!missingIds.isEmpty()) {
            List<DiaryResponse> dbResults =
                    diaryRepository.findAllById(missingIds).stream()
                            .map(diaryMapper::toResponse)
                            .toList();

            dbResults.forEach(response -> {
                diaryCacheRepository.put(response);
                cached.put(response.getId(), response);
            });
        }

        List<DiaryResponse> result = diaryIds.stream()
                .map(cached::get)
                .filter(Objects::nonNull)
                .filter(diary -> isOwner || isVisibleToUser(diary, isFriend))
                .toList();

        applyLiked(userId, result);

        return new PageImpl<>(result, pageable, docPage.getTotalElements());
    }

    private boolean isFriend(Long userId, Long targetUserId) {
        if (userId.equals(targetUserId)) {
            return true;
        }

        return followRepository.existsByFollowerIdAndFollowingId(userId, targetUserId)
                || followRepository.existsByFollowerIdAndFollowingId(targetUserId, userId);
    }

    private boolean isVisibleToUser(DiaryResponse diary, boolean isFriend) {
        String visibility = diary.getVisibility();

        return "PUBLIC".equalsIgnoreCase(visibility)
                || (isFriend && "FRIENDS".equalsIgnoreCase(visibility));
    }

    private void applyLiked(Long userId, List<DiaryResponse> result) {
        if (userId == null || result.isEmpty()) {
            result.forEach(response -> response.setIsLiked(false));
            return;
        }

        Set<Long> likedDiaryIds = new HashSet<>(
                likeRepository.findLikedDiaryIds(
                        userId,
                        result.stream()
                                .map(DiaryResponse::getId)
                                .toList()
                )
        );

        result.forEach(response ->
                response.setIsLiked(likedDiaryIds.contains(response.getId()))
        );
    }
}