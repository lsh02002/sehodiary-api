package com.shop.sehodiary_api.repository.user.userRoles;

import com.shop.sehodiary_api.repository.user.userRoles.Roles;
import com.shop.sehodiary_api.repository.user.userRoles.RolesRepository;
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
class RolesRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private RolesRepository rolesRepository;

    @Test
    @DisplayName("이름으로 Roles 조회")
    void findByName() {
        // given
        Roles role = Roles.builder()
                .name("ROLE_USER")
                .build();

        em.persist(role);
        em.flush();
        em.clear();

        // when
        Roles result = rolesRepository.findByName("ROLE_USER");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("Roles 저장 테스트")
    void saveRoles() {
        // given
        Roles role = Roles.builder()
                .name("ROLE_ADMIN")
                .build();

        // when
        Roles saved = rolesRepository.save(role);

        // then
        assertThat(saved.getRolesId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("logMessage 정상 동작")
    void logMessage() {
        // given
        Roles role = Roles.builder()
                .name("ROLE_MANAGER")
                .build();

        // when
        String message = role.logMessage();

        // then
        assertThat(message).isEqualTo("권한 인증 유형 'ROLE_MANAGER'");
    }
}