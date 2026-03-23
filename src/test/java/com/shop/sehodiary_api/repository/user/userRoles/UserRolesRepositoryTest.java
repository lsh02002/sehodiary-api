package com.shop.sehodiary_api.repository.user.userRoles;

import com.shop.sehodiary_api.TestUserFactory;
import com.shop.sehodiary_api.repository.user.User;
import jakarta.persistence.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@EnableJpaAuditing
class UserRolesRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private UserRolesRepository userRolesRepository;

    @Test
    @DisplayName("UserRoles 저장 및 연관관계 조회")
    void save_and_find() {
        // given
        User user = User.builder()
                .email("홍길동@email.com")
                .nickname("홍길동")
                .password("12341234aaaa")
                .userStatus("정상")
                .build();

        Roles role = Roles.builder()
                .name("ROLE_USER")
                .build();

        em.persist(user);
        em.persist(role);

        UserRoles userRoles = UserRoles.builder()
                .user(user)
                .roles(role)
                .build();

        em.persist(userRoles);
        em.flush();
        em.clear();

        // when
        UserRoles result = userRolesRepository.findById(userRoles.getUserRolesId()).orElseThrow();

        // then
        assertThat(result.getUser().getNickname()).isEqualTo("홍길동");
        assertThat(result.getRoles().getName()).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("logMessage 정상 동작")
    void logMessage() {
        // given
        User user = User.builder()
                .nickname("김개발")
                .build();

        Roles role = Roles.builder()
                .name("ROLE_ADMIN")
                .build();

        UserRoles userRoles = UserRoles.builder()
                .user(user)
                .roles(role)
                .build();

        // when
        String message = userRoles.logMessage();

        // then
        assertThat(message).isEqualTo("사용자 김개발에게 'ROLE_ADMIN'");
    }
}