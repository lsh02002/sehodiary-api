package com.shop.sehodiary_api.repository.diary;

import com.shop.sehodiary_api.repository.activity.logger.Loggable;
import com.shop.sehodiary_api.repository.comment.Comment;
import com.shop.sehodiary_api.repository.common.BaseTimeEntity;
import com.shop.sehodiary_api.repository.common.Visibility;
import com.shop.sehodiary_api.repository.diaryEmotion.DiaryEmotion;
import com.shop.sehodiary_api.repository.diaryImage.DiaryImage;
import com.shop.sehodiary_api.repository.like.Like;
import com.shop.sehodiary_api.repository.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "diaries")
public class Diary extends BaseTimeEntity implements Loggable, Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "visibility", nullable = false, length = 20)
    private Visibility visibility = Visibility.PRIVATE;

    @Column(name = "weather", length = 50)
    private String weather;

    @Builder.Default
    @OneToMany(mappedBy = "diary", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DiaryImage> diaryImages = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "diary", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DiaryEmotion> diaryEmotions = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "diary", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Comment> comments = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "diary", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Like> likes = new ArrayList<>();

    public Diary(User user, String title, String content, Visibility visibility, String weather) {
        this.user = user;
        this.title = title;
        this.content = content;
        this.visibility = (visibility == null) ? Visibility.PRIVATE : visibility;
        this.weather = weather;
    }

    @Override
    public String logMessage() {
        return "글 '" + title + "'";
    }

    public void addDiaryEmotion(DiaryEmotion diaryEmotion) {
        diaryEmotions.add(diaryEmotion);
        diaryEmotion.setDiary(this);
    }

    public void addComment(Comment comment) {
        comments.add(comment);
        comment.setDiary(this);
    }

    public void removeComment(Comment comment) {
        comments.remove(comment);
        comment.setDiary(null);
    }

    public void removeLike(Like like) {
        likes.remove(like);
        like.setDiary(null);
    }
}
