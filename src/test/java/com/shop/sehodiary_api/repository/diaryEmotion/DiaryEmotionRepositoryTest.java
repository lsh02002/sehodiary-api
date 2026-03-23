package com.shop.sehodiary_api.repository.diaryEmotion;

import com.shop.sehodiary_api.TestUserFactory;
import com.shop.sehodiary_api.repository.common.Visibility;
import com.shop.sehodiary_api.repository.diary.Diary;
import com.shop.sehodiary_api.repository.emotion.Emotion;
import com.shop.sehodiary_api.repository.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@EnableJpaAuditing
class DiaryEmotionRepositoryTest {
    private static final AtomicLong COUNTER = new AtomicLong();

    @Autowired
    private DiaryEmotionRepository diaryEmotionRepository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("findByDiaryId: diaryId에 해당하는 DiaryEmotion 목록을 조회한다")
    void findByDiaryId() {
        User user = createUser("test1@email.com");
        Diary diary1 = createDiary(user, "제목1");
        Diary diary2 = createDiary(user, "제목2");

        Emotion happy = createEmotion(user,"HAPPY", "😊");
        Emotion sad = createEmotion(user,"SAD", "😢");
        Emotion angry = createEmotion(user,"ANGRY", "😠");

        DiaryEmotion diaryEmotion1 = createDiaryEmotion(diary1, happy);
        DiaryEmotion diaryEmotion2 = createDiaryEmotion(diary1, sad);
        createDiaryEmotion(diary2, angry);

        em.flush();
        em.clear();

        List<DiaryEmotion> result = diaryEmotionRepository.findByDiaryId(diary1.getId());

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(DiaryEmotion::getId)
                .containsExactlyInAnyOrder(diaryEmotion1.getId(), diaryEmotion2.getId());
    }

    @Test
    @DisplayName("findByDiaryId: 해당 diary에 연결된 감정이 없으면 빈 리스트를 반환한다")
    void findByDiaryId_returnsEmptyList() {
        User user = createUser("test1@email.com");
        Diary diary = createDiary(user, "제목1");

        em.flush();
        em.clear();

        List<DiaryEmotion> result = diaryEmotionRepository.findByDiaryId(diary.getId());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByDiaryIdAndEmotionName: diaryId와 emotionName이 일치하면 조회된다")
    void findByDiaryIdAndEmotionName() {
        User user = createUser("test1@email.com");
        Diary diary = createDiary(user, "제목1");

        Emotion happy = createEmotion(user, "HAPPY", "😊");
        Emotion sad = createEmotion(user,"SAD", "😢");

        DiaryEmotion saved = createDiaryEmotion(diary, happy);
        createDiaryEmotion(diary, sad);

        em.flush();
        em.clear();

        Optional<DiaryEmotion> result =
                diaryEmotionRepository.findByDiaryIdAndEmotionName(diary.getId(), "HAPPY");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    @DisplayName("findByDiaryIdAndEmotionName: emotionName이 다르면 조회되지 않는다")
    void findByDiaryIdAndEmotionName_returnsEmptyWhenEmotionNameDifferent() {
        User user = createUser("test1@email.com");
        Diary diary = createDiary(user, "제목1");

        Emotion happy = createEmotion(user, "HAPPY", "😊");
        createDiaryEmotion(diary, happy);

        em.flush();
        em.clear();

        Optional<DiaryEmotion> result =
                diaryEmotionRepository.findByDiaryIdAndEmotionName(diary.getId(), "SAD");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByDiaryIdAndEmotionName: diaryId가 다르면 조회되지 않는다")
    void findByDiaryIdAndEmotionName_returnsEmptyWhenDiaryDifferent() {
        User user = createUser("test1@email.com");
        Diary diary1 = createDiary(user, "제목1");
        Diary diary2 = createDiary(user, "제목2");

        Emotion happy = createEmotion(user, "HAPPY", "😊");
        createDiaryEmotion(diary1, happy);

        em.flush();
        em.clear();

        Optional<DiaryEmotion> result =
                diaryEmotionRepository.findByDiaryIdAndEmotionName(diary2.getId(), "HAPPY");

        assertThat(result).isEmpty();
    }

    private User createUser(String email) {
        User user = User.builder()
                .email(email)
                .nickname("lsh02002"+COUNTER.incrementAndGet())
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
                .content("asdfasdfasfd")
                .build();
        em.persist(diary);
        return diary;
    }

    private Emotion createEmotion(User user, String name, String emoji) {
        Emotion emotion = Emotion.builder()
                .name(name)
                .creator(user)
                .emoji(emoji)
                .build();
        em.persist(emotion);
        return emotion;
    }

    private DiaryEmotion createDiaryEmotion(Diary diary, Emotion emotion) {
        DiaryEmotion diaryEmotion = DiaryEmotion.builder()
                .diary(diary)
                .emotion(emotion)
                .build();
        em.persist(diaryEmotion);
        return diaryEmotion;
    }
}