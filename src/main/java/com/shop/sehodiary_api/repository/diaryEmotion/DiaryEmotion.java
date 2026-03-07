package com.shop.sehodiary_api.repository.diaryEmotion;

import com.shop.sehodiary_api.repository.activity.logger.Loggable;
import com.shop.sehodiary_api.repository.common.BaseTimeEntity;
import com.shop.sehodiary_api.repository.diary.Diary;
import com.shop.sehodiary_api.repository.emotion.Emotion;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "diary_emotions",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_diary_emotions_diary_emotion", columnNames = {"diary_id", "emotion_id"})
    }
)
public class DiaryEmotion extends BaseTimeEntity implements Loggable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "diary_id", nullable = false)
    private Diary diary;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emotion_id", nullable = false)
    private Emotion emotion;

    public DiaryEmotion(Diary diary, Emotion emotion) {
        this.diary = diary;
        this.emotion = emotion;
    }

    @Override
    public String logMessage() {
        return "글 '" + diary.getTitle() + "' 에 이모지 " + emotion.getName() + " '" + emotion.getEmoji() + "'";
    }
}
