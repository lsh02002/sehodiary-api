package com.shop.sehodiary_api.service.like;

import com.shop.sehodiary_api.repository.activity.function.SnapshotFunc;
import com.shop.sehodiary_api.repository.activity.ActivityAction;
import com.shop.sehodiary_api.repository.activity.ActivityEntityType;
import com.shop.sehodiary_api.repository.diary.Diary;
import com.shop.sehodiary_api.repository.diary.DiaryCacheRepository;
import com.shop.sehodiary_api.repository.diary.DiaryRepository;
import com.shop.sehodiary_api.repository.like.Like;
import com.shop.sehodiary_api.repository.like.LikeRepository;
import com.shop.sehodiary_api.repository.user.User;
import com.shop.sehodiary_api.repository.user.UserRepository;
import com.shop.sehodiary_api.service.activelog.ActivityLogService;
import com.shop.sehodiary_api.service.exceptions.ConflictException;
import com.shop.sehodiary_api.service.exceptions.NotFoundException;
import com.shop.sehodiary_api.web.dto.diary.DiaryResponse;
import com.shop.sehodiary_api.web.mapper.diary.DiaryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LikeService {
    private final UserRepository userRepository;
    private final DiaryRepository diaryRepository;
    private final LikeRepository likeRepository;
    private final DiaryMapper diaryMapper;
    private final ActivityLogService activityLogService;
    private final SnapshotFunc snapshotFunc;

    private final DiaryCacheRepository diaryCacheRepository;

    @Transactional
    public List<String> getLikingNicknamesByDiary(Long diaryId) {
        return likeRepository.findByDiaryId(diaryId)
                .stream().map(like->like.getUser().getNickname()).toList();
    }

    @Transactional
    public Boolean isLiked(Long userId, Long diaryId){
        return likeRepository.existsByUserIdAndDiaryId(userId, diaryId);
    }

    @Transactional
    public Boolean insert(Long userId, Long diaryId) {

        User user =userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("해당 사용자를 찾을 수 없습니다.", userId));

        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new NotFoundException("해당 글을 찾을 수 없습니다.", diaryId));

        // 이미 좋아요되어있으면 에러 반환
        if(likeRepository.findByUserIdAndDiaryId(userId, diaryId).isPresent()) {
                throw new ConflictException("이미 좋아요가 되어있습니다.", null);
        }

        Like like = Like.builder()
                .diary(diary)
                .user(user)
                .build();

        likeRepository.save(like);

        Object afterLike = snapshotFunc.snapshot(like);

        activityLogService.log(ActivityEntityType.LIKE, ActivityAction.CREATE, like.getId(), like.logMessage(), user, null, afterLike);

        DiaryResponse response = diaryMapper.toResponse(like.getDiary());

        diaryCacheRepository.put(response);

        return true;
    }

    @Transactional
    public Boolean delete(Long userId, Long diaryId) {

        User user =userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("해당 사용자를 찾을 수 없습니다.", userId));

        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(()-> new NotFoundException("해당 글을 찾을 수 없습니다.", null));

        Like like = likeRepository.findByUserIdAndDiaryId(userId, diaryId)
                .orElseThrow(() -> new NotFoundException("좋아요가 되어있지 않았습니다.", null));

        Object beforeLike = snapshotFunc.snapshot(like);

        activityLogService.log(ActivityEntityType.LIKE, ActivityAction.DELETE, like.getId(), like.logMessage(), user, beforeLike, null);

        diary.removeLike(like);
        likeRepository.delete(like);

        DiaryResponse response = diaryMapper.toResponse(diary);

        diaryCacheRepository.put(response);

        return false;
    }

    @Transactional(readOnly = true)
    public List<DiaryResponse> getMyLikedDiaries(Long userId) {
        return likeRepository.findAllByUserId(userId)
                .stream().map(like->diaryMapper.toResponse(like.getDiary())).toList();
    }
}
