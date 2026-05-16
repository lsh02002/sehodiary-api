package com.shop.sehodiary_api.service.diary;

import com.shop.sehodiary_api.repository.activity.ActivityAction;
import com.shop.sehodiary_api.repository.activity.ActivityEntityType;
import com.shop.sehodiary_api.repository.activity.function.SnapshotFunc;
import com.shop.sehodiary_api.repository.common.Visibility;
import com.shop.sehodiary_api.repository.diary.Diary;
import com.shop.sehodiary_api.repository.diary.DiaryCacheRepository;
import com.shop.sehodiary_api.repository.diary.DiaryIdRedisRepository;
import com.shop.sehodiary_api.repository.diary.DiaryRepository;
import com.shop.sehodiary_api.repository.follow.FollowRepository;
import com.shop.sehodiary_api.repository.like.LikeRepository;
import com.shop.sehodiary_api.repository.user.User;
import com.shop.sehodiary_api.repository.user.UserRepository;
import com.shop.sehodiary_api.repository.user.userDetails.CustomUserDetails;
import com.shop.sehodiary_api.service.activelog.ActivityLogService;
import com.shop.sehodiary_api.service.diaryemotion.DiaryEmotionService;
import com.shop.sehodiary_api.service.diaryimage.DiaryImageService;
import com.shop.sehodiary_api.service.exceptions.AccessDeniedException;
import com.shop.sehodiary_api.service.exceptions.ConflictException;
import com.shop.sehodiary_api.service.exceptions.NotAcceptableException;
import com.shop.sehodiary_api.service.exceptions.NotFoundException;
import com.shop.sehodiary_api.service.webpush.WebPushService;
import com.shop.sehodiary_api.web.dto.diary.DiaryRequest;
import com.shop.sehodiary_api.web.dto.diary.DiaryResponse;
import com.shop.sehodiary_api.web.dto.fcm.PostCreatedEvent;
import com.shop.sehodiary_api.web.mapper.diary.DiaryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryService {
    private final UserRepository userRepository;
    private final DiaryRepository diaryRepository;
    private final LikeRepository likeRepository;
    private final DiaryMapper diaryMapper;
    private final ActivityLogService activityLogService;
    private final DiaryImageService diaryImageService;
    private final DiaryEmotionService diaryEmotionService;
    private final SnapshotFunc snapshotFunc;

    private final DiaryIdRedisRepository diaryIdRedisRepository;
    private final DiaryCacheRepository diaryCacheRepository;
    private final FollowRepository followRepository;

    private final WebPushService webPushService;
    private final ApplicationEventPublisher eventPublisher;

//    private final DiarySearchIndexer diarySearchIndexer;

    @Transactional(readOnly = true)
    public Page<DiaryResponse> getDiariesByPublic(Long userId, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = start + pageable.getPageSize() - 1;

        boolean exists = diaryIdRedisRepository.existsPublicKey();
        List<Long> publicIds = diaryIdRedisRepository.findPublicPage(start, end);

        if (!exists) {
            synchronized (this) {
                exists = diaryIdRedisRepository.existsPublicKey();
                if (!exists) {
                    List<Long> allIds = diaryRepository.findAllPublicIds();
                    if (!allIds.isEmpty()) {
                        diaryIdRedisRepository.savePublicIds(allIds);
                    }
                }
            }

            publicIds = diaryIdRedisRepository.findPublicPage(start, end);
        }

        if (publicIds.isEmpty()) {
            return Page.empty(pageable);
        }

        Map<Long, DiaryResponse> cached = diaryCacheRepository.getAllByIds(publicIds);

        List<Long> missingIds = publicIds.stream()
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

        List<DiaryResponse> result = publicIds.stream()
                .map(cached::get)
                .filter(Objects::nonNull)
                .filter(response -> Visibility.PUBLIC.toString().equals(response.getVisibility()))
                .collect(Collectors.toList());

        long total = diaryIdRedisRepository.countPublicIds();

        if (userId == null || result.isEmpty()) {
            result.forEach(response -> response.setIsLiked(false));
            return new PageImpl<>(result, pageable, total);
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

        return new PageImpl<>(result, pageable, total);
    }

    @Transactional(readOnly = true)
    public Page<DiaryResponse> getDiariesByFriends(Long userId, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = start + pageable.getPageSize() - 1;

        boolean exists = diaryIdRedisRepository.existsFriendsKey();
        List<Long> friendsIds = diaryIdRedisRepository.findFriendsPage(start, end);

        if (!exists) {
            synchronized (this) {
                exists = diaryIdRedisRepository.existsFriendsKey();
                if (!exists) {
                    List<Long> allIds = diaryRepository.findAllFriendsIds();
                    if (!allIds.isEmpty()) {
                        diaryIdRedisRepository.saveFriends(allIds);
                    }
                }
            }

            friendsIds = diaryIdRedisRepository.findFriendsPage(start, end);
        }

        if (friendsIds.isEmpty()) {
            return Page.empty(pageable);
        }

        Map<Long, DiaryResponse> cached = diaryCacheRepository.getAllByIds(friendsIds);

        List<Long> missingIds = friendsIds.stream()
                .filter(id -> !cached.containsKey(id))
                .toList();

        if (!missingIds.isEmpty()) {
            List<DiaryResponse> dbResults = diaryRepository.findAllById(missingIds).stream()
                    .filter(diary -> diary.getVisibility() == Visibility.FRIENDS)
                    .map(diaryMapper::toResponse)
                    .toList();

            dbResults.forEach(response -> {
                diaryCacheRepository.put(response);
                cached.put(response.getId(), response);
            });
        }

        List<DiaryResponse> result = friendsIds.stream()
                .map(cached::get)
                .filter(Objects::nonNull)
                .filter(response -> Visibility.FRIENDS.toString().equals(response.getVisibility()))
                .collect(Collectors.toList());

        long total = diaryIdRedisRepository.countFriendsIds();

        if (userId == null || result.isEmpty()) {
            result.forEach(response -> response.setIsLiked(false));
            return new PageImpl<>(result, pageable, total);
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

        return new PageImpl<>(result, pageable, total);
    }

    @Transactional(readOnly = true)
    public Page<DiaryResponse> getDiariesByUser(Long targetUserId, Long loginUserId, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = start + pageable.getPageSize() - 1;

        boolean exists = diaryIdRedisRepository.existsUserKey(targetUserId);
        List<Long> diaryIds = diaryIdRedisRepository.findUserPage(targetUserId, start, end);

        if (!exists) {
            synchronized (this) {
                exists = diaryIdRedisRepository.existsUserKey(targetUserId);
                if (!exists) {
                    List<Long> allIds = diaryRepository.findIdsByUserId(targetUserId);
                    if (!allIds.isEmpty()) {
                        diaryIdRedisRepository.saveUserIds(targetUserId, allIds);
                    }
                }
            }

            diaryIds = diaryIdRedisRepository.findUserPage(targetUserId, start, end);
        }

        if (diaryIds.isEmpty()) {
            return Page.empty(pageable);
        }

        Map<Long, DiaryResponse> cached = diaryCacheRepository.getAllByIds(diaryIds);

        List<Long> missingIds = diaryIds.stream()
                .filter(id -> !cached.containsKey(id))
                .toList();

        if (!missingIds.isEmpty()) {
            List<DiaryResponse> dbResults = diaryRepository.findAllById(missingIds).stream()
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
                .collect(Collectors.toList());

        long total = diaryIdRedisRepository.countUserIds(targetUserId);

        if (loginUserId == null || result.isEmpty()) {
            result.forEach(response -> response.setIsLiked(false));
            return new PageImpl<>(result, pageable, total);
        }

        Set<Long> likedDiaryIds = new HashSet<>(
                likeRepository.findLikedDiaryIds(
                        loginUserId,
                        result.stream().map(DiaryResponse::getId).toList()
                )
        );

        result.forEach(response ->
                response.setIsLiked(likedDiaryIds.contains(response.getId()))
        );

        return new PageImpl<>(result, pageable, total);
    }

    @Transactional(readOnly = true)
    public Page<DiaryResponse> getDiariesPublicAndFriendsByUser(Long userId, Long targetUserId, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = start + pageable.getPageSize() - 1;

        boolean exists = diaryIdRedisRepository.existsUserKey(targetUserId);
        List<Long> diaryIds = diaryIdRedisRepository.findUserPage(targetUserId, start, end);

        if (!exists) {
            synchronized (this) {
                exists = diaryIdRedisRepository.existsUserKey(targetUserId);
                if (!exists) {
                    List<Long> allIds = diaryRepository.findIdsByUserId(targetUserId);
                    if (!allIds.isEmpty()) {
                        diaryIdRedisRepository.saveUserIds(targetUserId, allIds);
                    }
                }
            }

            diaryIds = diaryIdRedisRepository.findUserPage(targetUserId, start, end);
        }

        if (diaryIds.isEmpty()) {
            return Page.empty(pageable);
        }

        boolean isFriend = isFriend(userId, targetUserId);

        Map<Long, DiaryResponse> cached = diaryCacheRepository.getAllByIds(diaryIds);

        List<Long> missingIds = diaryIds.stream()
                .filter(id -> !cached.containsKey(id))
                .toList();

        if (!missingIds.isEmpty()) {
            List<DiaryResponse> dbResults = diaryRepository.findAllById(missingIds).stream()
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
                .filter(diary -> isVisibleToUser(diary, isFriend))
                .collect(Collectors.toList());

        long total = diaryIdRedisRepository.countUserIds(targetUserId);

        if (userId == null || result.isEmpty()) {
            result.forEach(diary -> diary.setIsLiked(false));
            return new PageImpl<>(result, pageable, total);
        }

        Set<Long> likedDiaryIds = new HashSet<>(
                likeRepository.findLikedDiaryIds(
                        userId,
                        result.stream().map(DiaryResponse::getId).toList()
                )
        );

        result.forEach(diary ->
                diary.setIsLiked(likedDiaryIds.contains(diary.getId()))
        );

        return new PageImpl<>(result, pageable, total);
    }

    @Transactional(readOnly = true)
    public DiaryResponse getOneDiary(Long diaryId) {
        String currentUserNickname = getCurrentUserNickname();

        Optional<DiaryResponse> cached = diaryCacheRepository.get(diaryId);
        if (cached.isPresent()) {
            DiaryResponse response = cached.get();

            validateDiaryAccess(
                    Visibility.valueOf(response.getVisibility()),
                    response.getNickname(),
                    currentUserNickname
            );

            return response;
        }

        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new NotFoundException("해당 글을 찾을 수 없습니다.", diaryId));

        validateDiaryAccess(
                diary.getVisibility(),
                diary.getUser().getNickname(),
                currentUserNickname
        );

        DiaryResponse response = diaryMapper.toResponse(diary);
        diaryCacheRepository.put(response);

        return response;
    }

    @Transactional
    public DiaryResponse createDiary(Long userId, DiaryRequest request, List<MultipartFile> files) {
        User user = userRepository.findById(userId)
                        .orElseThrow(()-> new NotFoundException("해당 사용자를 찾을 수 없습니다.", userId));

        if(request.getTitle().trim().isEmpty()) {
            throw new NotAcceptableException("해당 제목란이 비어있습니다", request.getTitle());
        }

        if(request.getContent().trim().isEmpty()) {
            throw new NotAcceptableException("해당 내용란이 비어있습니다", request.getContent());
        }

        if (request.getDate() == null) {
            throw new NotAcceptableException("날짜가 비어있습니다.", null);
        }

        LocalDate newDate;
        try {
            newDate = LocalDate.parse(request.getDate());
        } catch (DateTimeParseException e) {
            throw new NotAcceptableException("올바르지 않은 날짜 형식입니다.", request.getDate());
        }

        if(request.getVisibility().trim().isEmpty()) {
            throw new NotAcceptableException("해당 공개여부란이 비어있습니다", request.getVisibility());
        }

        if(request.getWeather().trim().isEmpty()) {
            throw new NotAcceptableException("해당 날씨란이 비어있습니다", request.getWeather());
        }

        Diary diary = Diary.builder()
                .user(user)
                .title(request.getTitle())
                .content(request.getContent())
                .date(newDate)
                .visibility(Visibility.from(request.getVisibility()))
                .weather(request.getWeather())
                .build();

        diaryRepository.save(diary);

        diaryImageService.uploadManyFiles(userId, diary.getId(), files);
        diaryEmotionService.createDiaryEmotion(userId, diary.getId(), request.getEmoji());

        Object afterDiary = snapshotFunc.snapshot(diary);
        activityLogService.log(ActivityEntityType.DIARY, ActivityAction.CREATE, diary.getId(), diary.logMessage(), user, null, afterDiary);

        DiaryResponse response = diaryMapper.toResponse(diary);

        diaryCacheRepository.put(response);

//        diarySearchIndexer.index(diary);

        if (diary.getVisibility() == Visibility.PUBLIC) {
            syncDiaryIdsByPublic(diary.getId());

//            webPushService.broadcast(
//                    "새 글이 등록됐어요",
//                    "'" + diary.getTitle() + "' 새 글이 올라왔습니다.",
//                    "/edit/" + diary.getId()
//            );

            eventPublisher.publishEvent(
                    new PostCreatedEvent(
                            diary.getId(),
                            diary.getUser().getId().toString(),
                            diary.getTitle(),
                            diary.getContent()
                    )
            );

        } else if (diary.getVisibility() == Visibility.FRIENDS) {
            syncDiaryIdsByFriends(diary.getId());
        }
        syncDiaryIdsByUser(userId, diary.getId());

        return response;
    }

    @Transactional
    public DiaryResponse editDiary(Long userId, Long diaryId, DiaryRequest request, List<MultipartFile> files) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("해당 사용자를 찾을 수 없습니다.", userId));

        diaryRepository.findById(diaryId)
                .orElseThrow(() -> new NotFoundException("해당 글을 찾을 수 없습니다.", diaryId));

        Diary diary = diaryRepository.findByUserIdAndId(userId, diaryId)
                .orElseThrow(() -> new NotFoundException("해당 사용자가 작성한 글이 아닙니다", diaryId));

        Object beforeDiary = snapshotFunc.snapshot(diary);

        if(request.getTitle().trim().isEmpty()) {
            throw new NotAcceptableException("해당 제목란이 비어있습니다", request.getTitle());
        }

        if(request.getContent().trim().isEmpty()) {
            throw new NotAcceptableException("해당 내용란이 비어있습니다", request.getContent());
        }

        if (request.getDate() == null) {
            throw new NotAcceptableException("날짜가 비어있습니다.", null);
        }

        if(request.getVisibility().trim().isEmpty()) {
            throw new NotAcceptableException("해당 공개여부란이 비어있습니다", request.getVisibility());
        }

        if(request.getWeather().trim().isEmpty()) {
            throw new NotAcceptableException("해당 날씨란이 비어있습니다", request.getWeather());
        }

        if (!Objects.equals(diary.getTitle(), request.getTitle())) {
            diary.setTitle(request.getTitle());
        }

        if (!Objects.equals(diary.getContent(), request.getContent())) {
            diary.setContent(request.getContent());
        }

        LocalDate newDate;
        try {
            newDate = LocalDate.parse(request.getDate());
        } catch (DateTimeParseException e) {
            throw new NotAcceptableException("올바르지 않은 날짜 형식입니다.", request.getDate());
        }

        if (!diary.getDate().equals(newDate)) {
            diary.setDate(newDate);
        }

        Visibility newVisibility = Visibility.from(request.getVisibility());
        if (!Objects.equals(diary.getVisibility(), newVisibility)) {
            diary.setVisibility(newVisibility);
        }

        if (!Objects.equals(diary.getWeather(), request.getWeather())) {
            diary.setWeather(request.getWeather());
        }

        // if (files != null && !files.isEmpty()) {
            diaryImageService.uploadManyFiles(userId, diaryId, files);
        // }

        if (request.getEmoji() != null && !request.getEmoji().isBlank()) {
            diaryEmotionService.editDiaryEmotion(userId, diaryId, request.getEmoji());
        }

        diary.setUpdatedAt(LocalDateTime.now());

        diaryRepository.flush();

//        diarySearchIndexer.index(diary);

        Diary reloadedDiary = diaryRepository.findByUserIdAndId(userId, diaryId)
                .orElseThrow(() -> new NotFoundException("해당 사용자가 작성한 글이 아닙니다", diaryId));

        Object afterDiary = snapshotFunc.snapshot(reloadedDiary);
        activityLogService.log(
                ActivityEntityType.DIARY,
                ActivityAction.UPDATE,
                reloadedDiary.getId(),
                reloadedDiary.logMessage(),
                user,
                beforeDiary,
                afterDiary
        );

        DiaryResponse response = diaryMapper.toResponse(diary);

        diaryCacheRepository.put(response);

//        diarySearchIndexer.index(diary);

        if (diary.getVisibility() == Visibility.PUBLIC) {
            syncDiaryIdsByPublic(diary.getId());
        } else if (diary.getVisibility() == Visibility.FRIENDS) {
            syncDiaryIdsByFriends(diary.getId());
        }

        return response;
    }

    @Transactional
    public void deleteDiary(Long userId, Long diaryId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(()-> new NotFoundException("해당 사용자를 찾을 수 없습니다.", userId));

            diaryRepository.findById(diaryId)
                    .orElseThrow(() -> new NotFoundException("해당 글을 찾을 수 없습니다.", diaryId));

            Diary diary = diaryRepository.findByUserIdAndId(userId, diaryId)
                    .orElseThrow(()->new NotFoundException("해당 사용자가 작성한 글이 아닙니다", diaryId));

            diaryImageService.deleteManyFiles(diary);

            Object beforeDiary = snapshotFunc.snapshot(diary);
            activityLogService.log(ActivityEntityType.DIARY, ActivityAction.DELETE, diary.getId(), diary.logMessage(), user, beforeDiary, null);

            diaryRepository.deleteByUserIdAndId(userId, diaryId);

            diaryCacheRepository.delete(diaryId);
            diaryIdRedisRepository.remove(diaryId);
            diaryIdRedisRepository.removeFromUser(userId, diaryId);

//            diarySearchIndexer.index(diary);

        } catch (RuntimeException e) {
            throw new ConflictException("해당 글을 삭제할 수 없습니다", diaryId);
        }
    }

    public boolean isVisibleToUser(DiaryResponse diary, boolean isFriend) {
        String visibility = diary.getVisibility();

        return "PUBLIC".equalsIgnoreCase(visibility)
                || (isFriend && "FRIENDS".equalsIgnoreCase(visibility));
    }

    public boolean isFriend(Long userId, Long targetUserId) {
        if (userId.equals(targetUserId)) {
            return true;
        }

        return followRepository.existsByFollowerIdAndFollowingId(userId, targetUserId)
                || followRepository.existsByFollowerIdAndFollowingId(targetUserId, userId);
    }

    private void validateDiaryAccess(Visibility visibility, String writerNickname, String currentUserNickname) {
        if (visibility == Visibility.PRIVATE) {
            if (currentUserNickname == null || !writerNickname.equals(currentUserNickname)) {
                throw new AccessDeniedException("비공개 글은 작성자만 조회할 수 있습니다.", currentUserNickname);
            }
        }
    }

    private String getCurrentUserNickname() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 인증 정보 자체가 없거나 인증 안 된 경우 → 비로그인으로 간주
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        // 익명 사용자 (anonymousUser) 등 처리
        if (!(principal instanceof CustomUserDetails userDetails)) {
            return null;
        }

        return userDetails.getNickname();
    }

    private void syncDiaryIdsByPublic(Long diaryId) {
        if (diaryIdRedisRepository.addPublicIfPresent(diaryId)) {
            return;
        }

        List<Long> publicDiaryIds = diaryRepository.findAllPublicIds();
        diaryIdRedisRepository.savePublicIds(publicDiaryIds);
    }

    private void syncDiaryIdsByFriends(Long diaryId) {
        if(diaryIdRedisRepository.addFriendsIfPresent(diaryId)) {
            return;
        }

        List<Long> friendsDiaryIds = diaryRepository.findAllFriendsIds();
        diaryIdRedisRepository.saveFriends(friendsDiaryIds);
    }

    private void syncDiaryIdsByUser(Long userId, Long diaryId) {
        if(diaryIdRedisRepository.addUserIfPresent(userId, diaryId)) {
            return;
        }

        List<Long> userDiaryIds = diaryRepository.findIdsByUserId(userId);
        diaryIdRedisRepository.saveUserIds(userId, userDiaryIds);
    }
}
