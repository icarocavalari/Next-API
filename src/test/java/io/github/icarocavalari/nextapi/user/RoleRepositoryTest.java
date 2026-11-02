package io.github.icarocavalari.nextapi.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import io.github.icarocavalari.nextapi.TestcontainersConfiguration;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static io.github.icarocavalari.nextapi.user.RoleName.ADMIN;
import static io.github.icarocavalari.nextapi.user.RoleName.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
class RoleRepositoryTest {
    @Autowired
    private RoleRepository roleRepo;

    @Autowired
    private TestEntityManager testEntityManager;

    @Test
    void shouldFindUserAndAdminRole() {
        Optional<Role> userRole = roleRepo.findByName(USER);
        assertThat(userRole).isNotEmpty();
        assertThat(userRole.get().getName()).isEqualTo(USER);

        Optional<Role> adminRole = roleRepo.findByName(ADMIN);
        assertThat(adminRole).isNotEmpty();
        assertThat(adminRole.get().getName()).isEqualTo(ADMIN);
    }

    @Test
    void shouldNotDeleteRoleBeingUsed() {
        Role userRole = roleRepo.findByName(USER).orElseThrow();

        testEntityManager.persistAndFlush(new User("test@email.com", "passwordtest", "nickname", 16, userRole));
        testEntityManager.clear();

        Role role = roleRepo.findByName(USER).orElseThrow();

        assertThatThrownBy(() -> {
            roleRepo.delete(role);
            roleRepo.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);

    }
}