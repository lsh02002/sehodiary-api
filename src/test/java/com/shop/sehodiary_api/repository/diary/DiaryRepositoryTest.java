package com.shop.sehodiary_api.repository.diary;

import com.shop.sehodiary_api.repository.common.Visibility;
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
class DiaryRepositoryTest {
    private static final AtomicLong COUNTER = new AtomicLong();

    @Autowired
    private DiaryRepository diaryRepository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("findByUserIdAndId: userId와 diaryId가 모두 일치하는 Diary를 조회한다")
    void findByUserIdAndId() {
        User user = createUser("test1@email.com");
        User otherUser = createUser("test2@email.com");

        Diary diary = createDiary(user, Visibility.PUBLIC);
        createDiary(otherUser, Visibility.PUBLIC);

        em.flush();
        em.clear();

        Optional<Diary> result = diaryRepository.findByUserIdAndId(user.getId(), diary.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(diary.getId());
        assertThat(result.get().getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("findByUserIdAndId: userId가 다르면 조회되지 않는다")
    void findByUserIdAndId_returnsEmpty() {
        User user = createUser("test1@email.com");
        User otherUser = createUser("test2@email.com");

        Diary diary = createDiary(user, Visibility.PUBLIC);

        em.flush();
        em.clear();

        Optional<Diary> result = diaryRepository.findByUserIdAndId(otherUser.getId(), diary.getId());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("deleteByUserIdAndId: userId와 diaryId가 일치하는 Diary를 삭제한다")
    void deleteByUserIdAndId() {
        User user = createUser("test1@email.com");
        Diary diary = createDiary(user, Visibility.PUBLIC);

        em.flush();
        em.clear();

        diaryRepository.deleteByUserIdAndId(user.getId(), diary.getId());
        em.flush();
        em.clear();

        Optional<Diary> result = diaryRepository.findById(diary.getId());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllPublicIds: PUBLIC 공개범위 diary id만 조회한다")
    void findAllPublicIds() {
        User user = createUser("test1@email.com");

        Diary publicDiary1 = createDiary(user, Visibility.PUBLIC);
        Diary publicDiary2 = createDiary(user, Visibility.PUBLIC);
        createDiary(user, Visibility.FRIENDS);

        em.flush();
        em.clear();

        List<Long> result = diaryRepository.findAllPublicIds();

        assertThat(result).contains(publicDiary1.getId(), publicDiary2.getId());
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("findAllFriendsIds: FRIENDS 공개범위 diary id만 조회한다")
    void findAllFriendsIds() {
        User user = createUser("test1@email.com");

        Diary friendsDiary1 = createDiary(user, Visibility.FRIENDS);
        Diary friendsDiary2 = createDiary(user, Visibility.FRIENDS);
        createDiary(user, Visibility.PUBLIC);

        em.flush();
        em.clear();

        List<Long> result = diaryRepository.findAllFriendsIds();

        assertThat(result).contains(friendsDiary1.getId(), friendsDiary2.getId());
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("findIdsByUserId: 해당 user의 diary id만 조회한다")
    void findIdsByUserId() {
        User user1 = createUser("test1@email.com");
        User user2 = createUser("test2@email.com");

        Diary user1Diary1 = createDiary(user1, Visibility.PUBLIC);
        Diary user1Diary2 = createDiary(user1, Visibility.FRIENDS);
        createDiary(user2, Visibility.PUBLIC);

        em.flush();
        em.clear();

        List<Long> result = diaryRepository.findIdsByUserId(user1.getId());

        assertThat(result).containsExactlyInAnyOrder(user1Diary1.getId(), user1Diary2.getId());
        assertThat(result).hasSize(2);
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

    private Diary createDiary(User user, Visibility visibility) {
        Diary diary = Diary.builder()
                .user(user)
                .title("title" + COUNTER.incrementAndGet())
                .content("asdfasdfasfd")
                .visibility(visibility)
                .build();
        em.persist(diary);
        return diary;
    }
}