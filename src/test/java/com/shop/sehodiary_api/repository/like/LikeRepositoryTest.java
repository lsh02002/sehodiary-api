package com.shop.sehodiary_api.repository.like;

import com.shop.sehodiary_api.TestUserFactory;
import com.shop.sehodiary_api.repository.diary.Diary;
import com.shop.sehodiary_api.repository.user.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@EnableJpaAuditing
class LikeRepositoryTest {

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("findByUserIdAndDiaryId: userId와 diaryId가 일치하면 Like를 반환한다")
    void findByUserIdAndDiaryId() {
        User user1 = createUser("user1@test.com", "user1");
        User user2 = createUser("user2@test.com", "user2");

        Diary diary1 = createDiary(user1, "제목1");
        Diary diary2 = createDiary(user2, "제목2");

        Like savedLike = createLike(diary1, user2);
        createLike(diary2, user1);

        em.flush();
        em.clear();

        Optional<Like> result = likeRepository.findByUserIdAndDiaryId(user2.getId(), diary1.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(savedLike.getId());
    }

    @Test
    @DisplayName("findByUserIdAndDiaryId: 일치하는 데이터가 없으면 Optional.empty를 반환한다")
    void findByUserIdAndDiaryId_returnsEmpty() {
        User user1 = createUser("user1@test.com", "user1");
        User user2 = createUser("user2@test.com", "user2");

        Diary diary1 = createDiary(user1, "제목1");
        Diary diary2 = createDiary(user2, "제목2");

        createLike(diary1, user2);

        em.flush();
        em.clear();

        Optional<Like> result = likeRepository.findByUserIdAndDiaryId(user1.getId(), diary2.getId());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("existsByUserIdAndDiaryId: 일치하는 좋아요가 있으면 true를 반환한다")
    void existsByUserIdAndDiaryId_returnsTrue() {
        User user1 = createUser("user1@test.com", "user1");
        User user2 = createUser("user2@test.com", "user2");

        Diary diary = createDiary(user1, "제목1");
        createLike(diary, user2);

        em.flush();
        em.clear();

        Boolean result = likeRepository.existsByUserIdAndDiaryId(user2.getId(), diary.getId());

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("existsByUserIdAndDiaryId: 일치하는 좋아요가 없으면 false를 반환한다")
    void existsByUserIdAndDiaryId_returnsFalse() {
        User user1 = createUser("user1@test.com", "user1");
        User user2 = createUser("user2@test.com", "user2");

        Diary diary = createDiary(user1, "제목1");

        em.flush();
        em.clear();

        Boolean result = likeRepository.existsByUserIdAndDiaryId(user2.getId(), diary.getId());

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("findAllByUserId: 해당 사용자가 누른 좋아요 목록을 반환한다")
    void findAllByUserId() {
        User author1 = createUser("author1@test.com", "author1");
        User author2 = createUser("author2@test.com", "author2");
        User liker = createUser("liker@test.com", "liker");

        Diary diary1 = createDiary(author1, "제목1");
        Diary diary2 = createDiary(author2, "제목2");
        Diary diary3 = createDiary(author1, "제목3");

        Like like1 = createLike(diary1, liker);
        Like like2 = createLike(diary2, liker);
        createLike(diary3, author2);

        em.flush();
        em.clear();

        List<Like> result = likeRepository.findAllByUserId(liker.getId());

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(Like::getId)
                .containsExactlyInAnyOrder(like1.getId(), like2.getId());
    }

    @Test
    @DisplayName("findAllByUserId: 해당 사용자의 좋아요가 없으면 빈 리스트를 반환한다")
    void findAllByUserId_returnsEmpty() {
        User author = createUser("author@test.com", "author");
        User liker = createUser("liker@test.com", "liker");

        Diary diary = createDiary(author, "제목1");
        createLike(diary, author);

        em.flush();
        em.clear();

        List<Like> result = likeRepository.findAllByUserId(liker.getId());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByDiaryId: 해당 diary의 좋아요 목록을 반환한다")
    void findByDiaryId() {
        User author = createUser("tes1@email.com", "test1");
        User user1 = createUser("test2@email.com", "test2");
        User user2 = createUser("test3@email.com", "test3");

        Diary diary1 = createDiary(author, "제목1");
        Diary diary2 = createDiary(author, "제목2");

        Like like1 = createLike(diary1, user1);
        Like like2 = createLike(diary1, user2);
        createLike(diary2, user1);

        em.flush();
        em.clear();

        List<Like> result = likeRepository.findByDiaryId(diary1.getId());

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(Like::getId)
                .containsExactlyInAnyOrder(like1.getId(), like2.getId());
    }

    @Test
    @DisplayName("findByDiaryId: 해당 diary의 좋아요가 없으면 빈 리스트를 반환한다")
    void findByDiaryId_returnsEmpty() {
        User author = createUser("test1@email.com", "test1");
        Diary diary = createDiary(author, "제목1");

        em.flush();
        em.clear();

        List<Like> result = likeRepository.findByDiaryId(diary.getId());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("같은 user와 diary 조합의 좋아요는 중복 저장할 수 없다")
    void duplicateLikeNotAllowed() {
        User author = createUser("author@test.com", "author");
        User liker = createUser("liker@test.com", "liker");

        em.persist(author);
        em.persist(liker);

        Diary diary = createDiary(author, "제목1");
        em.persist(diary);

        createLike(diary, liker);

        assertThatThrownBy(() -> {
            createLike(diary, liker);
            em.flush();
        }).isInstanceOf(PersistenceException.class);
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

    private Diary createDiary(User user, String title) {
        Diary diary = Diary.builder()
                .user(user)
                .title(title)
                .content("내용")
                .build();
        em.persist(diary);
        return diary;
    }

    private Like createLike(Diary diary, User user) {
        Like like = Like.builder()
                .diary(diary)
                .user(user)
                .build();
        em.persist(like);
        return like;
    }
}