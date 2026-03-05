package com.shop.sehodiary_api.repository.emotion;

import java.util.ArrayList;
import java.util.List;

import com.shop.sehodiary_api.repository.activity.logger.Loggable;
import com.shop.sehodiary_api.repository.common.BaseTimeEntity;
import com.shop.sehodiary_api.repository.diaryEmotion.DiaryEmotion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "emotions",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_emotions_name", columnNames = {"emotion_name"})
    }
)
public class Emotion extends BaseTimeEntity implements Loggable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "emotion_name", nullable = false, length = 50)
    private String name;

    @Column(name = "emoji", length = 20)
    private String emoji;

    @OneToMany(mappedBy = "emotion", fetch = FetchType.LAZY)
    private List<DiaryEmotion> diaryEmotions = new ArrayList<>();

    public Emotion(String name, String emoji) {
        this.name = name;
        this.emoji = emoji;
    }

    @Override
    public String logMessage() {
        return "name=";
    }
}
