package com.shop.sehodiary_api.repository.emotion;

import java.util.ArrayList;
import java.util.List;

import com.shop.sehodiary_api.repository.activity.logger.Loggable;
import com.shop.sehodiary_api.repository.common.BaseTimeEntity;
import com.shop.sehodiary_api.repository.diaryEmotion.DiaryEmotion;
import com.shop.sehodiary_api.repository.user.User;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Column(name = "emotion_name", unique = true, nullable = false, length = 50)
    private String name;

    @Column(name = "emoji", unique = true, nullable = false, length = 20)
    private String emoji;

    @Builder.Default
    @OneToMany(mappedBy = "emotion", fetch = FetchType.LAZY)
    private List<DiaryEmotion> diaryEmotions = new ArrayList<>();

    public Emotion(User creator, String name, String emoji) {
        this.creator = creator;
        this.name = name;
        this.emoji = emoji;
    }

    @Override
    public String logMessage() {
        return "이모지 " + name + " '" + emoji + "'" ;
    }
}
