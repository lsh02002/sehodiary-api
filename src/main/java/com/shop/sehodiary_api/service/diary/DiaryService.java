package com.shop.sehodiary_api.service.diary;

import com.shop.sehodiary_api.repository.activity.function.SnapshotFunc;
import com.shop.sehodiary_api.repository.activity.ActivityAction;
import com.shop.sehodiary_api.repository.activity.ActivityEntityType;
import com.shop.sehodiary_api.repository.common.Visibility;
import com.shop.sehodiary_api.repository.diary.Diary;
import com.shop.sehodiary_api.repository.diary.DiaryCacheRepository;
import com.shop.sehodiary_api.repository.diary.DiaryIdRedisRepository;
import com.shop.sehodiary_api.repository.diary.DiaryRepository;
import com.shop.sehodiary_api.repository.follow.FollowRepository;
import com.shop.sehodiary_api.repository.user.User;
import com.shop.sehodiary_api.repository.user.UserRepository;
import com.shop.sehodiary_api.repository.user.userDetails.CustomUserDetails;
import com.shop.sehodiary_api.service.activelog.ActivityLogService;
import com.shop.sehodiary_api.service.diaryemotion.DiaryEmotionService;
import com.shop.sehodiary_api.service.diaryimage.DiaryImageService;
import com.shop.sehodiary_api.service.exceptions.*;
import com.shop.sehodiary_api.web.dto.diary.DiaryRequest;
import com.shop.sehodiary_api.web.dto.diary.DiaryResponse;
import com.shop.sehodiary_api.web.mapper.diary.DiaryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiaryService {
    private final UserRepository userRepository;
    private final DiaryRepository diaryRepository;
    private final DiaryMapper diaryMapper;
    private final ActivityLogService activityLogService;
    private final DiaryImageService diaryImageService;
    private final DiaryEmotionService diaryEmotionService;
    private final SnapshotFunc snapshotFunc;

    private final DiaryIdRedisRepository diaryIdRedisRepository;
    private final DiaryCacheRepository diaryCacheRepository;
    private final FollowRepository followRepository;

    @Transactional(readOnly = true)
    public List<DiaryResponse> getDiariesByPublic() {
        List<Long> publicIds = diaryIdRedisRepository.findAllPublic();
        Map<Long, DiaryResponse> cached = diaryCacheRepository.getAll();

        // publicIds가 비어있으면 DB에서 채우기
        if (publicIds.isEmpty()) {
            List<Long> ids = diaryRepository.findAllPublicIds();

            diaryIdRedisRepository.savePublicIds(ids);
            publicIds = new ArrayList<>(ids);
        }

        List<DiaryResponse> result = publicIds.stream()
                .map(cached::get)
                .filter(Objects::nonNull)
                .filter(cache -> Objects.equals(cache.getVisibility(), Visibility.PUBLIC.toString()))
                .collect(Collectors.toCollection(ArrayList::new));

        List<Long> missingIds = publicIds.stream()
                .filter(id -> !cached.containsKey(id))
                .toList();

        if (!missingIds.isEmpty()) {
            List<DiaryResponse> dbResults = diaryRepository.findAllById(missingIds).stream()
                    .filter(diary -> diary.getVisibility() == Visibility.PUBLIC)
                    .map(diaryMapper::toResponse)
                    .toList();

            dbResults.forEach(diaryCacheRepository::put);
            result.addAll(dbResults);
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<DiaryResponse> getDiariesByFriends() {
        List<Long> friendIds = diaryIdRedisRepository.findAllFriends();
        Map<Long, DiaryResponse> cached = diaryCacheRepository.getAll();

        if (friendIds.isEmpty()) {
            List<Long> ids = diaryRepository.findAllFriendsIds();

            diaryIdRedisRepository.saveFriends(ids);
            friendIds = new ArrayList<>(ids);
        }

        List<DiaryResponse> result = friendIds.stream()
                .map(cached::get)
                .filter(Objects::nonNull)
                .filter(cache -> Objects.equals(cache.getVisibility(), Visibility.FRIENDS.toString()))
                .collect(Collectors.toCollection(ArrayList::new));

        List<Long> missingIds = friendIds.stream()
                .filter(id -> !cached.containsKey(id))
                .toList();

        if (!missingIds.isEmpty()) {
            List<DiaryResponse> dbResults = diaryRepository.findAllById(missingIds).stream()
                    .filter(diary -> diary.getVisibility() == Visibility.FRIENDS)
                    .map(diaryMapper::toResponse)
                    .toList();

            dbResults.forEach(diaryCacheRepository::put);
            result.addAll(dbResults);
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<DiaryResponse> getDiariesByUser(Long userId) {
        List<Long> diaryIds = diaryIdRedisRepository.findAllUser(userId);

        // user diaryIds가 비어있으면 DB에서 채우기
        if (diaryIds.isEmpty()) {
            List<Long> ids = diaryRepository.findIdsByUserId(userId);

            if (!ids.isEmpty()) {
                diaryIdRedisRepository.saveUserIds(userId, ids);
                diaryIds = new ArrayList<>(ids);
            } else {
                return List.of();
            }
        }

        Map<Long, DiaryResponse> cached = diaryCacheRepository.getAll();

        List<DiaryResponse> result = diaryIds.stream()
                .map(cached::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));

        List<Long> missingIds = diaryIds.stream()
                .filter(id -> !cached.containsKey(id))
                .toList();

        if (!missingIds.isEmpty()) {
            List<DiaryResponse> dbResults = diaryRepository.findAllById(missingIds).stream()
                    .map(diaryMapper::toResponse)
                    .toList();

            dbResults.forEach(diaryCacheRepository::put);
            result.addAll(dbResults);
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<DiaryResponse> getDiariesPublicAndFriendsByUser(Long userId, Long targetUserId) {
        List<Long> diaryIds = diaryIdRedisRepository.findAllUser(targetUserId);

        if (diaryIds.isEmpty()) {
            List<Long> ids = diaryRepository.findIdsByUserId(targetUserId);

            if (ids.isEmpty()) {
                return List.of();
            }

            diaryIdRedisRepository.saveUserIds(targetUserId, ids);
            diaryIds = new ArrayList<>(ids);
        }

        boolean isFriend = isFriend(userId, targetUserId);

        Map<Long, DiaryResponse> cached = diaryCacheRepository.getAll();

        List<DiaryResponse> result = diaryIds.stream()
                .map(cached::get)
                .filter(Objects::nonNull)
                .filter(diary -> isVisibleToUser(diary, isFriend))
                .collect(Collectors.toCollection(ArrayList::new));

        List<Long> missingIds = diaryIds.stream()
                .filter(id -> !cached.containsKey(id))
                .toList();

        if (!missingIds.isEmpty()) {
            List<DiaryResponse> dbResults = diaryRepository.findAllById(missingIds).stream()
                    .map(diaryMapper::toResponse)
                    .filter(diary -> isVisibleToUser(diary, isFriend))
                    .toList();

            dbResults.forEach(diaryCacheRepository::put);
            result.addAll(dbResults);
        }

        return result;
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

        if (diary.getVisibility() == Visibility.PUBLIC) {
            diaryIdRedisRepository.addPublic(diary.getId());
        } else if (diary.getVisibility() == Visibility.FRIENDS) {
            diaryIdRedisRepository.addFriends(diary.getId());
        }
        diaryIdRedisRepository.addUser(userId, diary.getId());

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

        diaryRepository.flush();

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

        if (diary.getVisibility() == Visibility.PUBLIC) {
            diaryIdRedisRepository.addPublic(diary.getId());
        } else if (diary.getVisibility() == Visibility.FRIENDS) {
            diaryIdRedisRepository.addFriends(diary.getId());
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
        if (visibility == Visibility.PRIVATE && !writerNickname.equals(currentUserNickname)) {
            throw new AccessDeniedException("비공개 글은 작성자만 조회할 수 있습니다.", currentUserNickname);
        }
    }

    private String getCurrentUserNickname() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CustomBadCredentialsException("로그인이 필요합니다.", null);
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof CustomUserDetails userDetails)) {
            throw new CustomBadCredentialsException("인증 정보를 찾을 수 없습니다.", null);
        }

        return userDetails.getNickname();
    }
}
