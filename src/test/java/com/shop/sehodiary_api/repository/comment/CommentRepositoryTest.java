package com.shop.sehodiary_api.repository.comment;

import com.shop.sehodiary_api.TestUserFactory;
import com.shop.sehodiary_api.repository.diary.Diary;
import com.shop.sehodiary_api.repository.user.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@EnableJpaAuditing
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("userId와 commentId로 댓글을 조회한다")
    void findByUserIdAndId() {
        // given
        User user = persistUser("user1@test.com");
        User otherUser = persistUser("user2@test.com");

        Diary diary = persistDiary(user, "테스트 일기");

        Comment comment = commentRepository.save(new Comment(diary, user, "내 댓글"));
        Comment otherComment = commentRepository.save(new Comment(diary, otherUser, "남의 댓글"));

        em.flush();
        em.clear();

        // when
        Optional<Comment> result = commentRepository.findByUserIdAndId(user.getId(), comment.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(comment.getId());
        assertThat(result.get().getUser().getId()).isEqualTo(user.getId());
        assertThat(result.get().getContent()).isEqualTo("내 댓글");
    }

    @Test
    @DisplayName("userId가 다르면 빈 값 반환")
    void findByUserIdAndId_returnsEmpty_whenUserDoesNotMatch() {
        // given
        User user = persistUser("user1@test.com");
        User otherUser = persistUser("user2@test.com");

        Diary diary = persistDiary(user, "테스트 일기");
        Comment comment = commentRepository.save(new Comment(diary, user, "내 댓글"));

        em.flush();
        em.clear();

        // when
        Optional<Comment> result = commentRepository.findByUserIdAndId(otherUser.getId(), comment.getId());

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("diaryId로 해당 일기의 모든 댓글 id를 조회한다")
    void findAllIdsByDiaryId() {
        // given
        User user = persistUser("user1@test.com");

        Diary diary1 = persistDiary(user, "일기1");
        Diary diary2 = persistDiary(user, "일기2");

        Comment c1 = commentRepository.save(new Comment(diary1, user, "댓글1"));
        Comment c2 = commentRepository.save(new Comment(diary1, user, "댓글2"));
        Comment c3 = commentRepository.save(new Comment(diary2, user, "댓글3"));

        em.flush();
        em.clear();

        // when
        List<Long> ids = commentRepository.findAllIdsByDiaryId(diary1.getId());

        // then
        assertThat(ids).containsExactlyInAnyOrder(c1.getId(), c2.getId());
        assertThat(ids).doesNotContain(c3.getId());
    }

    @Test
    @DisplayName("userId로 해당 유저의 모든 댓글 id를 조회한다")
    void findIdsByUserId() {
        // given
        User user1 = persistUser("user1@test.com");
        User user2 = persistUser("user2@test.com");

        Diary diary1 = persistDiary(user1, "일기1");
        Diary diary2 = persistDiary(user2, "일기2");

        Comment c1 = commentRepository.save(new Comment(diary1, user1, "user1 댓글1"));
        Comment c2 = commentRepository.save(new Comment(diary1, user1, "user1 댓글2"));
        Comment c3 = commentRepository.save(new Comment(diary2, user2, "user2 댓글1"));

        em.flush();
        em.clear();

        // when
        List<Long> ids = commentRepository.findIdsByUserId(user1.getId());

        // then
        assertThat(ids).containsExactlyInAnyOrder(c1.getId(), c2.getId());
        assertThat(ids).doesNotContain(c3.getId());
    }

    private User persistUser(String email) {
        // TODO: 실제 User 엔티티의 필수값에 맞게 수정
        User user = TestUserFactory.createUser();

        em.persist(user);
        return user;
    }

    private Diary persistDiary(User user, String title) {
        // TODO: 실제 Diary 엔티티의 필수값에 맞게 수정
        Diary diary = TestUserFactory.createDiary(user);

        em.persist(diary);
        return diary;
    }
}
