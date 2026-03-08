package com.shop.sehodiary_api.service.emotion;

import com.shop.sehodiary_api.repository.activity.ActivityAction;
import com.shop.sehodiary_api.repository.activity.ActivityEntityType;
import com.shop.sehodiary_api.repository.activity.function.SnapshotFunc;
import com.shop.sehodiary_api.repository.emotion.Emotion;
import com.shop.sehodiary_api.repository.emotion.EmotionRepository;
import com.shop.sehodiary_api.repository.user.User;
import com.shop.sehodiary_api.repository.user.UserRepository;
import com.shop.sehodiary_api.repository.user.userRoles.Roles;
import com.shop.sehodiary_api.repository.user.userRoles.UserRoles;
import com.shop.sehodiary_api.service.activelog.ActivityLogService;
import com.shop.sehodiary_api.service.exceptions.BadRequestException;
import com.shop.sehodiary_api.service.exceptions.ConflictException;
import com.shop.sehodiary_api.service.exceptions.NotAcceptableException;
import com.shop.sehodiary_api.service.exceptions.NotFoundException;
import com.shop.sehodiary_api.web.dto.emotion.EmotionRequest;
import com.shop.sehodiary_api.web.dto.emotion.EmotionResponse;
import com.shop.sehodiary_api.web.mapper.emotion.EmotionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class EmotionService {
    private final UserRepository userRepository;
    private final EmotionRepository emotionRepository;
    private final ActivityLogService activityLogService;
    private final SnapshotFunc snapshotFunc;
    private final EmotionMapper emotionMapper;

    @Transactional
    public List<EmotionResponse> getAllEmotions() {
        return emotionRepository.findAll()
                .stream().map(emotionMapper::toResponse).toList();
    }

    @Transactional
    public EmotionResponse createEmotion(Long userId, EmotionRequest request) {
        //따로 리액트로 생성 기능을 만들지 않을것임!!! POSTMAN으로 입력할 수 있음!!!
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new NotFoundException("해당 사용자를 찾을 수 없습니다.", userId));

        List<String> roles = user.getUserRoles().stream()
                .map(UserRoles::getRoles).map(Roles::getName).toList();

        if(!roles.contains("ROLE_ADMIN")){
            throw new BadRequestException("관리자 권한이 없습니다.", userId);
        }

        if(request.getName().trim().isEmpty()) {
            throw new NotAcceptableException("해당 이름란이 비어있습니다", request.getName());
        }

        if(request.getEmoji().trim().isEmpty()) {
            throw new NotAcceptableException("해당 이모지란이 비어있습니다", request.getEmoji());
        }

        if(emotionRepository.existsByName(request.getName())) {
            throw new NotAcceptableException("해당 이름이 이미 있습니다", request.getName());
        }

        if(emotionRepository.existsByEmoji(request.getEmoji())) {
            throw new NotAcceptableException("해당 이모지가 이미 있습니다", request.getEmoji());
        }

        Emotion emotion = Emotion.builder()
                .creator(user)
                .name(request.getName())
                .emoji(request.getEmoji())
                .build();

        emotionRepository.save(emotion);

        Object afterEmotion = snapshotFunc.snapshot(emotion);

        activityLogService.log(ActivityEntityType.EMOTION, ActivityAction.CREATE, emotion.getId(), emotion.logMessage(), user, null, afterEmotion);

        return emotionMapper.toResponse(emotion);
    }

    @Transactional
    public EmotionResponse editEmotion(Long userId, Long emotionId, EmotionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new NotFoundException("해당 사용자를 찾을 수 없습니다.", userId));

        List<String> roles = user.getUserRoles().stream()
                .map(UserRoles::getRoles).map(Roles::getName).toList();

        if(!roles.contains("ROLE_ADMIN")) {
            throw new BadRequestException("관리자 권한이 없습니다.", userId);
        }

        if(emotionRepository.existsByName(request.getName())) {
            throw new NotAcceptableException("해당 이름이 이미 있습니다", request.getName());
        }

        if(emotionRepository.existsByEmoji(request.getEmoji())) {
            throw new NotAcceptableException("해당 이모지가 이미 있습니다", request.getEmoji());
        }

        Emotion emotion = emotionRepository.findById(emotionId)
                .orElseThrow(()->new NotFoundException("해당 이모션을 찾을 수 없습니다", emotionId));

        Object beforeemotion = snapshotFunc.snapshot(emotion);

        if(!Objects.equals(emotion.getName(), request.getName())) {
            emotion.setName(request.getName());
        }

        if(!Objects.equals(emotion.getEmoji(), request.getEmoji())) {
            emotion.setEmoji(request.getEmoji());
        }

        Object afteremotion = snapshotFunc.snapshot(emotion);

        activityLogService.log(ActivityEntityType.EMOTION, ActivityAction.UPDATE, emotion.getId(), emotion.logMessage(), user, beforeemotion, afteremotion);
        return emotionMapper.toResponse(emotion);
    }

    @Transactional
    public void deleteEmotion(Long userId, Long emotionId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(()-> new NotFoundException("해당 사용자를 찾을 수 없습니다.", userId));

            List<String> roles = user.getUserRoles().stream()
                    .map(UserRoles::getRoles).map(Roles::getName).toList();

            if(!roles.contains("ROLE_ADMIN")) {
                throw new BadRequestException("사용자 권한이 없습니다.", userId);
            }

            Emotion emotion = emotionRepository.findById(emotionId)
                    .orElseThrow(()->new NotFoundException("해당 이모션을 찾을 수 없습니다", emotionId));

            Object beforeemotion = snapshotFunc.snapshot(emotion);

            activityLogService.log(ActivityEntityType.EMOTION, ActivityAction.DELETE, emotion.getId(), emotion.logMessage(), user, beforeemotion, null);

            emotionRepository.delete(emotion);
        } catch (RuntimeException e) {
            throw new ConflictException("해당 이모션을 삭제할 수 없습니다", emotionId);
        }
    }
}
