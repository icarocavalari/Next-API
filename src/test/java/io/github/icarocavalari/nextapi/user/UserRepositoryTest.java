package io.github.icarocavalari.nextapi.user;

import io.github.icarocavalari.nextapi.TestcontainersConfiguration;
import io.github.icarocavalari.nextapi.auth.identity.AuthProvider;
import io.github.icarocavalari.nextapi.auth.token.RefreshToken;
import io.github.icarocavalari.nextapi.auth.identity.UserIdentity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Duration;
import java.time.Instant;

import static io.github.icarocavalari.nextapi.user.RoleName.USER;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
public class UserRepositoryTest {
    @Autowired UserRepository userRepo;

    @Autowired RoleRepository roleRepo;

    @Autowired TestEntityManager testEntityManager;

    @Test
    void shouldGenerateIdAndCreatedAtFieldsAndSaveRole() {
        User savedUser = saveAndFlushUser("test@email.com", "nickname");

        testEntityManager.clear();

        User foundUser = userRepo.findByEmail("test@email.com").orElseThrow();

        assertThat(savedUser.getId()).isEqualTo(foundUser.getId());
        assertThat(foundUser.getCreatedAt()).isNotNull();
        assertThat(foundUser.getRoles()).extracting(Role::getName).containsExactly(USER);
    }

    @Test
    void shouldNotAcceptSavingUserWithEmailAlreadyTaken() {
        saveAndFlushUser("test@email.com", "nickname");

        assertThatThrownBy(() -> {
            saveAndFlushUser("test@email.com", "different");
        }).isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_users_email");
    }

    @Test
    void shouldAcceptUsersWithoutEmail() {
        assertThatCode(() -> {
            saveAndFlushUser(null, "first");
            saveAndFlushUser(null, "second");
        }).doesNotThrowAnyException();
    }

    @Test
    void shouldFindNicknamesIgnoringCase() {
        saveAndFlushUser(null, "FalleN");

        assertThat(userRepo.existsByNicknameIgnoreCase("FALLEN")).isTrue();
        assertThat(userRepo.existsByNicknameIgnoreCase("fallen")).isTrue();
        assertThat(userRepo.existsByNicknameIgnoreCase("fALLEn")).isTrue();
    }

    @Test
    void ShouldNotFindNicknameThatDoesNotExist() {
        saveAndFlushUser(null, "FalleN");
        assertThat(userRepo.existsByNicknameIgnoreCase("coldzera")).isFalse();
    }

    @Test
    void shouldRemoveAllIdentitiesAndTokensFromUserDeleted() {
        User savedUser = saveAndFlushUser("test@example.com", "nickname");

        UserIdentity userIdentity = testEntityManager.persistAndFlush(new UserIdentity(savedUser, AuthProvider.GOOGLE, "123456789"));
        RefreshToken refreshToken = testEntityManager.persistAndFlush(new RefreshToken("123456789", savedUser, Instant.now().plus(Duration.ofDays(7))));

        Long idUserIdentity = userIdentity.getId();
        Long idRefreshToken = refreshToken.getId();

        testEntityManager.clear();

        assertThat(testEntityManager.find(UserIdentity.class, idUserIdentity)).isNotNull();
        assertThat(testEntityManager.find(RefreshToken.class, idRefreshToken)).isNotNull();
        testEntityManager.clear();

        userRepo.deleteById(savedUser.getId());
        testEntityManager.flush();
        testEntityManager.clear();

        assertThat(testEntityManager.find(UserIdentity.class, idUserIdentity)).isNull();
        assertThat(testEntityManager.find(RefreshToken.class, idRefreshToken)).isNull();
    }

    private User saveAndFlushUser(String email, String nickname) {
        return userRepo.saveAndFlush(new User(email, "testpassword", nickname, 16, roleRepo.findByName(USER).orElseThrow()));
    }
}
