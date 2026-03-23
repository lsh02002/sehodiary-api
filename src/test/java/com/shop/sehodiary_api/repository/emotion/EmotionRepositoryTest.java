package com.shop.sehodiary_api.repository.emotion;

import com.shop.sehodiary_api.repository.user.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@EnableJpaAuditing
class EmotionRepositoryTest {

    @Autowired
    private EmotionRepository emotionRepository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("existsByName: 같은 이름의 감정이 존재하면 true를 반환한다")
    void existsByName_returnsTrue() {
        User creator = createUser("user1@test.com", "user1");
        createEmotion(creator, "HAPPY", "😊");

        em.flush();
        em.clear();

        boolean result = emotionRepository.existsByName("HAPPY");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("existsByName: 같은 이름의 감정이 없으면 false를 반환한다")
    void existsByName_returnsFalse() {
        User creator = createUser("user1@test.com", "user1");
        createEmotion(creator, "HAPPY", "😊");

        em.flush();
        em.clear();

        boolean result = emotionRepository.existsByName("SAD");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("existsByEmoji: 같은 이모지가 존재하면 true를 반환한다")
    void existsByEmoji_returnsTrue() {
        User creator = createUser("user1@test.com", "user1");
        createEmotion(creator, "HAPPY", "😊");

        em.flush();
        em.clear();

        boolean result = emotionRepository.existsByEmoji("😊");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("existsByEmoji: 같은 이모지가 없으면 false를 반환한다")
    void existsByEmoji_returnsFalse() {
        User creator = createUser("user1@test.com", "user1");
        createEmotion(creator, "HAPPY", "😊");

        em.flush();
        em.clear();

        boolean result = emotionRepository.existsByEmoji("😢");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("findByEmoji: 같은 이모지가 존재하면 Emotion을 반환한다")
    void findByEmoji_returnsEmotion() {
        User creator = createUser("user1@test.com", "user1");
        Emotion saved = createEmotion(creator, "HAPPY", "😊");

        em.flush();
        em.clear();

        Optional<Emotion> result = emotionRepository.findByEmoji("😊");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(saved.getId());
        assertThat(result.get().getName()).isEqualTo("HAPPY");
        assertThat(result.get().getEmoji()).isEqualTo("😊");
    }

    @Test
    @DisplayName("findByEmoji: 같은 이모지가 없으면 Optional.empty를 반환한다")
    void findByEmoji_returnsEmpty() {
        User creator = createUser("user1@test.com", "user1");
        createEmotion(creator, "HAPPY", "😊");

        em.flush();
        em.clear();

        Optional<Emotion> result = emotionRepository.findByEmoji("😢");

        assertThat(result).isEmpty();
    }

    private User createUser(String email, String nickname) {
        User user = User.builder()
                .email(email)
                .nickname(nickname)
                .password("12341234aaaa")
                .userStatus("정상")
                .build();
        em.persist(user);
        return user;
    }

    private Emotion createEmotion(User creator, String name, String emoji) {
        Emotion emotion = Emotion.builder()
                .creator(creator)
                .name(name)
                .emoji(emoji)
                .build();
        em.persist(emotion);
        return emotion;
    }
}