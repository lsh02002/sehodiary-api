package com.shop.sehodiary_api.repository.user;

import com.shop.sehodiary_api.repository.diary.Diary;
import com.shop.sehodiary_api.repository.diaryImage.DiaryImage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@EnableJpaAuditing
class UserRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("이메일로 사용자 조회")
    void findByEmail() {
        // given
        User user = User.builder()
                .email("test@example.com")
                .password("encoded-password")
                .nickname("tester")
                .userStatus("ACTIVE")
                .build();

        em.persist(user);
        em.flush();
        em.clear();

        // when
        Optional<User> result = userRepository.findByEmail("test@example.com");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("test@example.com");
        assertThat(result.get().getNickname()).isEqualTo("tester");
    }

    @Test
    @DisplayName("이메일 존재 여부 확인")
    void existsByEmail() {
        // given
        User user = User.builder()
                .email("exists@example.com")
                .password("encoded-password")
                .nickname("existsUser")
                .userStatus("ACTIVE")
                .build();

        em.persist(user);
        em.flush();
        em.clear();

        // when
        Boolean result = userRepository.existsByEmail("exists@example.com");

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("닉네임 존재 여부 확인")
    void existsByNickname() {
        // given
        User user = User.builder()
                .email("nick@example.com")
                .password("encoded-password")
                .nickname("uniqueNick")
                .userStatus("ACTIVE")
                .build();

        em.persist(user);
        em.flush();
        em.clear();

        // when
        Boolean result = userRepository.existsByNickname("uniqueNick");

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("logMessage 정상 동작")
    void logMessage() {
        // given
        User user = User.builder()
                .email("log@example.com")
                .password("encoded-password")
                .nickname("로그유저")
                .userStatus("ACTIVE")
                .build();

        // when
        String message = user.logMessage();

        // then
        assertThat(message).isEqualTo("사용자 '로그유저'");
    }

    @Test
    @DisplayName("addDiary 호출 시 연관관계가 설정된다")
    void addDiary() {
        // given
        User user = User.builder()
                .email("diary@example.com")
                .password("encoded-password")
                .nickname("diaryUser")
                .userStatus("ACTIVE")
                .build();

        Diary diary = Diary.builder().build();

        // when
        user.addDiary(diary);

        // then
        assertThat(user.getDiaries()).contains(diary);
        assertThat(diary.getUser()).isEqualTo(user);
    }

    @Test
    @DisplayName("removeDiary 호출 시 연관관계가 제거된다")
    void removeDiary() {
        // given
        User user = User.builder()
                .email("remove@example.com")
                .password("encoded-password")
                .nickname("removeUser")
                .userStatus("ACTIVE")
                .build();

        Diary diary = Diary.builder().build();
        user.addDiary(diary);

        // when
        user.removeDiary(diary);

        // then
        assertThat(user.getDiaries()).doesNotContain(diary);
        assertThat(diary.getUser()).isNull();
    }

    @Test
    @DisplayName("removeProfileImage 호출 시 연관관계가 제거된다")
    void removeProfileImage() {
        // given
        User user = User.builder()
                .email("image@example.com")
                .password("encoded-password")
                .nickname("imageUser")
                .userStatus("ACTIVE")
                .build();

        DiaryImage image = DiaryImage.builder().build();
        image.setProfileUser(user);
        user.getProfileImages().add(image);

        // when
        user.removeProfileImage(image);

        // then
        assertThat(user.getProfileImages()).doesNotContain(image);
        assertThat(image.getProfileUser()).isNull();
    }
}