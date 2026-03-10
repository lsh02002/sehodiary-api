package com.shop.sehodiary_api.repository.user;

import com.shop.sehodiary_api.repository.activity.logger.Loggable;
import com.shop.sehodiary_api.repository.comment.Comment;
import com.shop.sehodiary_api.repository.common.BaseTimeEntity;
import com.shop.sehodiary_api.repository.diary.Diary;
import com.shop.sehodiary_api.repository.diaryImage.DiaryImage;
import com.shop.sehodiary_api.repository.like.Like;
import com.shop.sehodiary_api.repository.user.userRoles.UserRoles;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User extends BaseTimeEntity implements Loggable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "nickname", unique = true, nullable = false, length = 50)
    private String nickname;

    @OneToMany(mappedBy = "user")
    private Collection<UserRoles> userRoles;

    @Column(nullable = false)
    private String userStatus;

    @Column
    private LocalDateTime deletedAt;

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Diary> diaries = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Comment> comments = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Like> likes = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "profileUser", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DiaryImage> profileImages = new ArrayList<>();

    public User(String email, String password, String nickname, String userStatus) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.userStatus = userStatus;
    }

    @Override
    public String logMessage() {
        return "사용자 '" + nickname + "'";
    }

    public void addDiary(Diary diary) {
        diaries.add(diary);
        diary.setUser(this);
    }

    public void removeDiary(Diary diary) {
        diaries.remove(diary);
        diary.setUser(null);
    }

    public void removeProfileImage(DiaryImage image) {
        profileImages.remove(image);
        image.setProfileUser(null);
    }
}
