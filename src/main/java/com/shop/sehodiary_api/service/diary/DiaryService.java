package com.shop.sehodiary_api.service.diary;

import com.shop.sehodiary_api.repository.activity.function.SnapshotFunc;
import com.shop.sehodiary_api.repository.activity.ActivityAction;
import com.shop.sehodiary_api.repository.activity.ActivityEntityType;
import com.shop.sehodiary_api.repository.common.Visibility;
import com.shop.sehodiary_api.repository.diary.Diary;
import com.shop.sehodiary_api.repository.diary.DiaryRepository;
import com.shop.sehodiary_api.repository.user.User;
import com.shop.sehodiary_api.repository.user.UserRepository;
import com.shop.sehodiary_api.service.activelog.ActivityLogService;
import com.shop.sehodiary_api.service.diaryimage.DiaryImageService;
import com.shop.sehodiary_api.service.exceptions.ConflictException;
import com.shop.sehodiary_api.service.exceptions.NotAcceptableException;
import com.shop.sehodiary_api.service.exceptions.NotFoundException;
import com.shop.sehodiary_api.web.dto.diary.DiaryRequest;
import com.shop.sehodiary_api.web.dto.diary.DiaryResponse;
import com.shop.sehodiary_api.web.mapper.diary.DiaryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DiaryService {
    private final UserRepository userRepository;
    private final DiaryRepository diaryRepository;
    private final DiaryMapper diaryMapper;
    private final ActivityLogService activityLogService;
    private final DiaryImageService diaryImageService;
    private final SnapshotFunc snapshotFunc;

    @Transactional
    public List<DiaryResponse> getDiariesByPublic() {
        return diaryRepository.findByVisibilityIn(List.of(Visibility.PUBLIC))
                .stream().map(diaryMapper::toResponse).toList();
    }

    @Transactional
    public List<DiaryResponse> getDiariesByFriends() {
        return diaryRepository.findByVisibilityIn(List.of(Visibility.PUBLIC, Visibility.FRIENDS))
                .stream().map(diaryMapper::toResponse).toList();
    }

    @Transactional
    public List<DiaryResponse> getDiariesByUser(Long userId) {
        return diaryRepository.findByUserId(userId)
                .stream().map(diaryMapper::toResponse).toList();
    }

    @Transactional
    public DiaryResponse getOneDiary(Long diaryId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(()-> new NotFoundException("해당 글을 찾을 수 없습니다.", diaryId));

        return diaryMapper.toResponse(diary);
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
                .visibility(Visibility.from(request.getVisibility()))
                .weather(request.getWeather())
                .build();

        diaryRepository.save(diary);

        diaryImageService.uploadManyFiles(userId, diary.getId(), files);

        Object afterDiary = snapshotFunc.snapshot(diary);

        activityLogService.log(ActivityEntityType.DIARY, ActivityAction.CREATE, diary.getId(), diary.logMessage(), user, null, afterDiary);

        return diaryMapper.toResponse(diary);
    }

    @Transactional
    public DiaryResponse editDiary(Long userId, Long diaryId, DiaryRequest request, List<MultipartFile> files) {
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new NotFoundException("해당 사용자를 찾을 수 없습니다.", userId));

        Diary diary = diaryRepository.findByUserIdAndId(userId, diaryId)
                .orElseThrow(()->new NotFoundException("해당 사용자가 작성한 글이 아닙니다", diaryId));

        Object beforeDiary = snapshotFunc.snapshot(diary);

        if(!Objects.equals(diary.getTitle(), request.getTitle())) {
            diary.setTitle(request.getTitle());
        }

        if(!Objects.equals(diary.getContent(), request.getContent())) {
            diary.setContent(request.getContent());
        }

        if(!Objects.equals(diary.getVisibility().toString(), request.getVisibility())) {
            diary.setVisibility(Visibility.from(request.getVisibility()));
        }

        if(!Objects.equals(diary.getWeather(), request.getWeather())) {
            diary.setWeather(request.getWeather());
        }

        diaryImageService.uploadManyFiles(userId, diaryId, files);

        diaryRepository.flush();
        Diary reloadedDiary = diaryRepository.findByUserIdAndId(userId, diaryId)
                .orElseThrow(() -> new NotFoundException("해당 사용자가 작성한 글이 아닙니다", diaryId));

        Object afterDiary = snapshotFunc.snapshot(reloadedDiary);

        activityLogService.log(ActivityEntityType.DIARY, ActivityAction.UPDATE, reloadedDiary.getId(), reloadedDiary.logMessage(), user, beforeDiary, afterDiary);
        return diaryMapper.toResponse(reloadedDiary);
    }

    @Transactional
    public void deleteDiary(Long userId, Long diaryId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(()-> new NotFoundException("해당 사용자를 찾을 수 없습니다.", userId));

            Diary diary = diaryRepository.findByUserIdAndId(userId, diaryId)
                    .orElseThrow(()->new NotFoundException("해당 사용자가 작성한 글이 아닙니다", diaryId));

            diaryImageService.deleteManyFiles(diary);

            Object beforeDiary = snapshotFunc.snapshot(diary);

            activityLogService.log(ActivityEntityType.DIARY, ActivityAction.DELETE, diary.getId(), diary.logMessage(), user, beforeDiary, null);

            diaryRepository.deleteByUserIdAndId(userId, diaryId);
        } catch (RuntimeException e) {
            throw new ConflictException("해당 글을 삭제할 수 업습니다", diaryId);
        }
    }
}
