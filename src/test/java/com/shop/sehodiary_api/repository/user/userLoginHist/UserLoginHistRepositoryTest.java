package com.shop.sehodiary_api.repository.user.userLoginHist;

import com.shop.sehodiary_api.TestUserFactory;
import com.shop.sehodiary_api.config.JpaAuditingTestConfig;
import com.shop.sehodiary_api.repository.user.User;
import jakarta.persistence.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingTestConfig.class)
@ActiveProfiles("test")
class UserLoginHistRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private UserLoginHistRepository userLoginHistRepository;

    @Test
    @DisplayName("userId로 로그인 이력을 페이징 조회한다")
    void findByUserId_withPaging() {
        // given
        User user = TestUserFactory.createUser();
        em.persist(user);

        for (int i = 0; i < 5; i++) {
            UserLoginHist hist = UserLoginHist.builder()
                    .loginAt(LocalDateTime.now())
                    .clientIp("127.0.0." + i)
                    .userAgent("agent-" + i)
                    .user(user)
                    .build();
            em.persist(hist);
        }

        em.flush();
        em.clear();

        Pageable pageable = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "loginAt"));

        // when
        Page<UserLoginHist> result =
                userLoginHistRepository.findByUserId(user.getId(), pageable);

        // then
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getContent().get(0).getUser().getId()).isEqualTo(user.getId());
    }
}