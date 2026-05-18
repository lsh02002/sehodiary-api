package com.shop.sehodiary_api.service.diarysearch;

import com.redis.om.spring.search.stream.EntityStream;
import com.redis.om.spring.search.stream.SearchStream;
import com.shop.sehodiary_api.config.redis.redissearch.DiarySearchDocument$;
import com.shop.sehodiary_api.config.redis.redissearch.DiarySearchDocument;
import com.shop.sehodiary_api.config.redis.redissearch.DiarySearchRepository;
import com.shop.sehodiary_api.repository.common.Visibility;
import com.shop.sehodiary_api.repository.diary.DiaryCacheRepository;
import com.shop.sehodiary_api.repository.diary.DiaryRepository;
import com.shop.sehodiary_api.repository.like.LikeRepository;
import com.shop.sehodiary_api.service.diary.DiaryService;
import com.shop.sehodiary_api.service.exceptions.NotAcceptableException;
import com.shop.sehodiary_api.web.dto.diary.DiaryResponse;
import com.shop.sehodiary_api.web.mapper.diary.DiaryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiarySearchService {

    private final EntityStream entityStream;
    private final DiaryCacheRepository diaryCacheRepository;
    private final DiaryRepository diaryRepository;
    private final DiaryMapper diaryMapper;
    private final LikeRepository likeRepository;

    private final DiaryService diaryService;

    private final DiarySearchRepository diarySearchRepository;

    @Transactional(readOnly = true)
    public Page<DiaryResponse> getDiariesByPublic(
            Long userId,
            String keyword,
            Pageable pageable
    ) {
        if (keyword != null && !keyword.isBlank()) {

            if (keyword.trim().length() < 2) {
                throw new NotAcceptableException("검색어는 2글자 이상 입력해주세요.", null);
            }

            return searchPublicDiaries(userId, keyword, pageable);
        }

        return diaryService.getDiariesByPublic(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<DiaryResponse> getDiariesPublicAndFriendsByUser(
            Long userId,
            Long targetUserId,
            String keyword,
            Pageable pageable
    ) {
        if (keyword != null && !keyword.trim().isBlank()) {

            if (keyword.trim().length() < 2) {
                throw new NotAcceptableException("검색어는 2글자 이상 입력해주세요.", null);
            }

            return searchDiariesPublicAndFriendsByUser(
                    userId,
                    targetUserId,
                    keyword.trim(),
                    pageable
            );
        }

        return diaryService.getDiariesPublicAndFriendsByUser(userId, targetUserId, pageable);
    }

    @Transactional
    public Page<DiaryResponse> searchPublicDiaries(
            Long userId,
            String keyword,
            Pageable pageable
    ) {
        String q = keyword.trim();

        SearchStream<DiarySearchDocument> stream =
                entityStream.of(DiarySearchDocument.class);

        List<DiarySearchDocument> docs = entityStream
                .of(DiarySearchDocument.class)
                .filter(DiarySearchDocument$.VISIBILITY.eq("PUBLIC"))
                .filter(
                        DiarySearchDocument$.TITLE.containing(q)
                                .or(DiarySearchDocument$.CONTENT.containing(q))
                )
                .collect(Collectors.toList())
                .stream()
                .sorted(
                        Comparator.comparing(DiarySearchDocument::getCreatedAt).reversed()
                                .thenComparing(
                                        Comparator.comparing(DiarySearchDocument::getDiaryId).reversed()
                                )
                )
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .toList();

        List<Long> diaryIds = docs.stream()
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
            List<DiaryResponse> dbResults = diaryRepository.findAllById(missingIds).stream()
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
                .filter(response -> "PUBLIC".equals(response.getVisibility()))
                .toList();

        applyLiked(userId, result);

        long total = entityStream.of(DiarySearchDocument.class)
                .filter(DiarySearchDocument$.VISIBILITY.eq("PUBLIC"))
                .filter(
                        DiarySearchDocument$.TITLE.containing(q)
                                .or(DiarySearchDocument$.CONTENT.containing(q))
                )
                .count();

        return new PageImpl<>(result, pageable, total);
    }

    @Transactional(readOnly = true)
    public Page<DiaryResponse> searchDiariesPublicAndFriendsByUser(
            Long userId,
            Long targetUserId,
            String keyword,
            Pageable pageable
    ) {
        boolean isOwner = Objects.equals(userId, targetUserId);
        boolean isFriend = diaryService.isFriend(userId, targetUserId);

        Set<String> allowedVisibilities;

        if (isOwner) {
            allowedVisibilities = Set.of(
                    Visibility.PRIVATE.name(),
                    Visibility.PUBLIC.name(),
                    Visibility.FRIENDS.name()
            );
        } else if (isFriend) {
            allowedVisibilities = Set.of(
                    Visibility.PUBLIC.name(),
                    Visibility.FRIENDS.name()
            );
        } else {
            allowedVisibilities = Set.of(
                    Visibility.PUBLIC.name()
            );
        }

        List<DiarySearchDocument> docPage = entityStream
                .of(DiarySearchDocument.class)
                .filter(DiarySearchDocument$.USER_ID.eq(targetUserId))
                .filter(
                        DiarySearchDocument$.TITLE.containing(keyword)
                                .or(DiarySearchDocument$.CONTENT.containing(keyword))
                )
                .collect(Collectors.toList())
                .stream()
                .filter(doc -> allowedVisibilities.contains(doc.getVisibility()))
                .sorted(
                        Comparator.comparing(DiarySearchDocument::getCreatedAt).reversed()
                                .thenComparing(
                                        Comparator.comparing(DiarySearchDocument::getDiaryId).reversed()
                                )
                )
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .toList();

        List<Long> diaryIds = docPage.stream()
                .filter(doc ->
                        Objects.equals(doc.getUserId(), targetUserId)
                                && allowedVisibilities.contains(doc.getVisibility())
                )
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
                .filter(diary -> isOwner || diaryService.isVisibleToUser(diary, isFriend))
                .toList();

        applyLiked(userId, result);

        return new PageImpl<>(result, pageable, docPage.size());
    }

    private void applyLiked(Long userId, List<DiaryResponse> result) {
        if (userId == null || result.isEmpty()) {
            result.forEach(response -> response.setIsLiked(false));
            return;
        }

        Set<Long> likedDiaryIds = new HashSet<>(
                likeRepository.findLikedDiaryIds(
                        userId,
                        result.stream().map(DiaryResponse::getId).toList()
                )
        );

        result.forEach(response ->
                response.setIsLiked(likedDiaryIds.contains(response.getId()))
        );
    }

    private String makeNgrams(String text) {
        if (text == null) return "";

        String normalized = text.replaceAll("\\s+", "");

        List<String> grams = new ArrayList<>();

        for (int i = 0; i < normalized.length(); i++) {
            for (int j = i + 1; j <= normalized.length(); j++) {
                grams.add(normalized.substring(i, j));
            }
        }

        return String.join(" ", grams);
    }
}
