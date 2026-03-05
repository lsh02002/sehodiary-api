package com.shop.sehodiary_api.repository.diary;

import java.util.ArrayList;
import java.util.List;

import com.shop.sehodiary_api.repository.activity.logger.Loggable;
import com.shop.sehodiary_api.repository.comment.Comment;
import com.shop.sehodiary_api.repository.common.BaseTimeEntity;
import com.shop.sehodiary_api.repository.common.Visibility;
import com.shop.sehodiary_api.repository.diaryEmotion.DiaryEmotion;
import com.shop.sehodiary_api.repository.diaryImage.DiaryImage;
import com.shop.sehodiary_api.repository.like.Like;
import com.shop.sehodiary_api.repository.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "diaries")
public class Diary extends BaseTimeEntity implements Loggable {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    private Visibility visibility = Visibility.PRIVATE;

    @Column(name = "weather", length = 50)
    private String weather;

    @OneToMany(mappedBy = "diary", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DiaryImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "diary", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DiaryEmotion> diaryEmotions = new ArrayList<>();

    @OneToMany(mappedBy = "diary", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Comment> comments = new ArrayList<>();

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
        return "name=";
    }

    public void addImage(DiaryImage image) {
        images.add(image);
        image.setDiary(this);
    }

    public void removeImage(DiaryImage image) {
        images.remove(image);
        image.setDiary(null);
    }

    public void addDiaryEmotion(DiaryEmotion diaryEmotion) {
        diaryEmotions.add(diaryEmotion);
        diaryEmotion.setDiary(this);
    }

    public void removeDiaryEmotion(DiaryEmotion diaryEmotion) {
        diaryEmotions.remove(diaryEmotion);
        diaryEmotion.setDiary(null);
    }

    public void addComment(Comment comment) {
        comments.add(comment);
        comment.setDiary(this);
    }

    public void removeComment(Comment comment) {
        comments.remove(comment);
        comment.setDiary(null);
    }

    public void addLike(Like like) {
        likes.add(like);
        like.setDiary(this);
    }

    public void removeLike(Like like) {
        likes.remove(like);
        like.setDiary(null);
    }
}
