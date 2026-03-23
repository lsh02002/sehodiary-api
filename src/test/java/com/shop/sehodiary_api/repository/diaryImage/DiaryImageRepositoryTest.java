package com.shop.sehodiary_api.repository.diaryImage;

import com.shop.sehodiary_api.TestUserFactory;
import com.shop.sehodiary_api.repository.diary.Diary;
import com.shop.sehodiary_api.repository.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@EnableJpaAuditing
class DiaryImageRepositoryTest {
    private static final AtomicLong COUNTER = new AtomicLong();

    @Autowired
    private DiaryImageRepository diaryImageRepository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("findByIdAndDeletedNot: deleted가 false인 경우 조회된다")
    void findByIdAndDeletedNot_success() {
        User user = createUser("test1@email.com");
        Diary diary = createDiary(user, "제목");

        DiaryImage image = createDiaryImage(diary, user, false);

        em.flush();
        em.clear();

        Optional<DiaryImage> result =
                diaryImageRepository.findByIdAndDeletedNot(image.getId(), true);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(image.getId());
    }

    @Test
    @DisplayName("findByIdAndDeletedNot: deleted 값이 같으면 조회되지 않는다")
    void findByIdAndDeletedNot_sameDeletedValue() {
        User user = createUser("test1@email.com");
        Diary diary = createDiary(user, "제목");

        DiaryImage image = createDiaryImage(diary, user, true);

        em.flush();
        em.clear();

        Optional<DiaryImage> result =
                diaryImageRepository.findByIdAndDeletedNot(image.getId(), true);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByIdAndDeletedNot: deleted가 null인 경우도 조회된다 (deleted != true)")
    void findByIdAndDeletedNot_deletedNull() {
        User user = createUser("test1@email.com");
        Diary diary = createDiary(user, "제목");

        DiaryImage image = createDiaryImage(diary, user, null);

        em.flush();
        em.clear();

        Optional<DiaryImage> result =
                diaryImageRepository.findByIdAndDeletedNot(image.getId(), true);

        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("findByIdAndDeletedNot: id가 다르면 조회되지 않는다")
    void findByIdAndDeletedNot_wrongId() {
        User user = createUser("test1@email.com");
        Diary diary = createDiary(user, "제목");

        createDiaryImage(diary, user, false);

        em.flush();
        em.clear();

        Optional<DiaryImage> result =
                diaryImageRepository.findByIdAndDeletedNot(999L, true);

        assertThat(result).isEmpty();
    }

    // =========================
    // helper
    // =========================

    private User createUser(String email) {
        User user = User.builder()
                .email(email)
                .nickname("nick" + COUNTER.incrementAndGet())
                .password("12341234aaaa")
                .userStatus("정상")
                .build();
        em.persist(user);
        return user;
    }

    private Diary createDiary(User user, String title) {
        Diary diary = Diary.builder()
                .user(user)
                .title(title)
                .content("내용")
                .build();
        em.persist(diary);
        return diary;
    }

    private DiaryImage createDiaryImage(Diary diary, User uploader, Boolean deleted) {
        DiaryImage image = DiaryImage.builder()
                .diary(diary)
                .uploader(uploader)
                .imageUrl("http://image.url")
                .fileName("file.jpg")
                .mimeType("image/jpeg")
                .sizeBytes(100L)
                .deleted(deleted)
                .build();

        em.persist(image);
        return image;
    }
}