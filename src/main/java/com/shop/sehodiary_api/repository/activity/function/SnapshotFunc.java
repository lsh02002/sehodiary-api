package com.shop.sehodiary_api.repository.activity.function;

import com.shop.sehodiary_api.repository.comment.Comment;
import com.shop.sehodiary_api.repository.diary.Diary;
import com.shop.sehodiary_api.repository.diaryEmotion.DiaryEmotion;
import com.shop.sehodiary_api.repository.diaryImage.DiaryImage;
import com.shop.sehodiary_api.repository.emotion.Emotion;
import com.shop.sehodiary_api.repository.like.Like;
import com.shop.sehodiary_api.repository.user.User;
import com.shop.sehodiary_api.repository.user.userRoles.Roles;
import com.shop.sehodiary_api.repository.user.userRoles.UserRoles;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class SnapshotFunc {
    public Map<String, Object> snapshot(Object obj) {
        if (obj == null) return null;

        // Comment 객체일 경우
        if (obj instanceof Comment comment) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", comment.getId());
            m.put("diaryId", comment.getDiary() != null ? comment.getDiary().getId() : null);
            m.put("userId", comment.getUser() != null ? comment.getUser().getId(): null);
            m.put("content", comment.getContent());
            m.put("createdAt", comment.getCreatedAt());
            m.put("updatedAt", comment.getUpdatedAt());
            return m;
        }

        if (obj instanceof Diary diary) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", diary.getId());
            m.put("userId", diary.getUser() != null ? diary.getUser().getId(): null);
            m.put("title", diary.getTitle());
            m.put("content", diary.getContent());
            m.put("visibility", diary.getVisibility() != null ? diary.getVisibility().name() : null);
            m.put("weather", diary.getWeather());
            m.put(
                    "diaryImages",
                    diary.getDiaryImages() == null
                            ? List.of()
                            : diary.getDiaryImages().stream()
                            .filter(image -> !image.getDeleted())
                            .map(DiaryImage::getImageUrl)
                            .toList()
            );
            m.put("diaryEmotions",
                    diary.getDiaryEmotions() == null
                            ? List.of()
                            : diary.getDiaryEmotions().stream()
                            .map(diaryEmotion -> diaryEmotion.getEmotion().getName())
                            .toList()
            );
            m.put("createdAt", diary.getCreatedAt());
            m.put("updatedAt", diary.getUpdatedAt());
            return m;
        }

        if(obj instanceof DiaryEmotion diaryEmotion) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", diaryEmotion.getId());
            m.put("diaryId", diaryEmotion.getDiary() != null ? diaryEmotion.getDiary().getId() : null);
            m.put("emotionId", diaryEmotion.getEmotion() != null ? diaryEmotion.getEmotion().getId() : null);
            m.put("createdAt", diaryEmotion.getCreatedAt());
            m.put("updatedAt", diaryEmotion.getUpdatedAt());
            return m;
        }

        if (obj instanceof DiaryImage diaryImage) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", diaryImage.getId());
            m.put("diaryId", diaryImage.getDiary() != null ? diaryImage.getDiary().getId() : null);
            m.put("profileUserId", diaryImage.getProfileUser() != null ? diaryImage.getProfileUser().getId() : null);
            m.put("uploaderId", diaryImage.getUploader() != null ? diaryImage.getUploader().getId() : null);
            m.put("imageUrl", diaryImage.getImageUrl());
            m.put("fileName", diaryImage.getFileName());
            m.put("mimeType", diaryImage.getMimeType());
            m.put("sizeBytes", diaryImage.getSizeBytes());
            m.put("deleted", diaryImage.getDeleted());
            m.put("createdAt", diaryImage.getCreatedAt());
            m.put("updatedAt", diaryImage.getUpdatedAt());
            return m;
        }

        if (obj instanceof Emotion emotion) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", emotion.getId());
            m.put("emotion_name", emotion.getName());
            m.put("emoji", emotion.getEmoji());
            m.put("createdAt", emotion.getCreatedAt());
            m.put("updatedAt", emotion.getUpdatedAt());
            return m;
        }

        if (obj instanceof Like like) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", like.getId());
            m.put("diaryId", like.getDiary() != null ? like.getDiary().getId() : null);
            m.put("userId", like.getUser() != null ? like.getUser().getId(): null);
            m.put("createdAt", like.getCreatedAt());
            m.put("updatedAt", like.getUpdatedAt());
            return m;
        }

        if (obj instanceof User user) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", user.getId());
            m.put("email", user.getEmail());
            m.put("password", user.getPassword());
            m.put("nickname", user.getNickname());
            m.put("profileImages", user.getProfileImages() != null ? user.getProfileImages().stream().filter(image -> !image.getDeleted()).map(DiaryImage::getImageUrl).toList() : null);
            m.put("userStatus", user.getUserStatus());
            m.put("deletedAt", user.getDeletedAt());
            m.put("createdAt", user.getCreatedAt());
            m.put("updatedAt", user.getUpdatedAt());
            return m;
        }

        if(obj instanceof Roles roles) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("rolesId", roles.getRolesId());
            m.put("name", roles.getName());
            m.put("createdAt", roles.getCreatedAt());
            m.put("updatedAt", roles.getUpdatedAt());
            return m;
        }

        if(obj instanceof UserRoles userRoles) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userRolesId", userRoles.getUserRolesId());
            m.put("userId", userRoles.getUser() != null ? userRoles.getUser().getId(): null);
            m.put("roleId", userRoles.getRoles() != null ? userRoles.getRoles().getRolesId(): null);
            m.put("createdAt", userRoles.getCreatedAt());
            m.put("updatedAt", userRoles.getUpdatedAt());
            return m;
        }

        return Map.of("value", String.valueOf(obj));
    }
}
