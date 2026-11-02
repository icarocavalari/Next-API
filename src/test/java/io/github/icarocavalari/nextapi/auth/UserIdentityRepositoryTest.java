package io.github.icarocavalari.nextapi.auth;

import io.github.icarocavalari.nextapi.TestcontainersConfiguration;
import io.github.icarocavalari.nextapi.user.RoleName;
import io.github.icarocavalari.nextapi.user.RoleRepository;
import io.github.icarocavalari.nextapi.user.User;
import io.github.icarocavalari.nextapi.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import static io.github.icarocavalari.nextapi.auth.AuthProvider.GOOGLE;
import static io.github.icarocavalari.nextapi.auth.AuthProvider.STEAM;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
public class UserIdentityRepositoryTest {
    @Autowired UserIdentityRepository userIdentityRepo;

    @Autowired UserRepository userRepo;

    @Autowired RoleRepository roleRepo;

    @Autowired TestEntityManager testEntityManager;

    private static final String PROVIDER_USER_ID = "123456789";

    @Test
    void shouldReturnCorrectUserIdentityAfterSaving() {
        User user = saveAndFlushUser("test@example.com", "nickname");
        UserIdentity saved = saveAndFlushUserIdentity(user, GOOGLE, PROVIDER_USER_ID);

        testEntityManager.clear();

        UserIdentity found = userIdentityRepo.findByProviderAndProviderUserId(
                GOOGLE, PROVIDER_USER_ID).orElseThrow();

        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getProvider()).isEqualTo(GOOGLE);
    }

    @Test
    void shouldNotReturnUserIdentityWithDifferentProvider() {
        User user = saveAndFlushUser("test@example.com", "nickname");
        saveAndFlushUserIdentity(user, GOOGLE, PROVIDER_USER_ID);

        assertThat(userIdentityRepo.findByProviderAndProviderUserId(STEAM, PROVIDER_USER_ID)).isEmpty();
    }

    @Test
    void shouldNotCreateTwoRowsWithSameProviderAndProviderUserId() {
        User first = saveAndFlushUser("first@example.com", "first");
        User second = saveAndFlushUser("second@example.com", "second");

        saveAndFlushUserIdentity(first, GOOGLE, PROVIDER_USER_ID);

        assertThatThrownBy(() -> {
            saveAndFlushUserIdentity(second, GOOGLE, PROVIDER_USER_ID);
        }).isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_user_identities_account");
    }

    @Test
    void shouldNotCreateTwoRowsWithSameProviderAndUser() {
        User savedUser = saveAndFlushUser("test@example.com", "nickname");
        saveAndFlushUserIdentity(savedUser, GOOGLE, PROVIDER_USER_ID);

        String differentProviderUserId = "987654321";

        assertThatThrownBy(() -> {
            saveAndFlushUserIdentity(savedUser, GOOGLE, differentProviderUserId);
        }).isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_user_identities_provider");
    }

    @Test
    void shouldAcceptSameProviderUserIdWithDifferentProviders() {
        User first = saveAndFlushUser("test@example.com", "nickname");
        saveAndFlushUserIdentity(first, GOOGLE, PROVIDER_USER_ID);

        User second = saveAndFlushUser("second@example.com", "second");
        assertThatCode(() -> {
            saveAndFlushUserIdentity(second, STEAM, PROVIDER_USER_ID);
        }).doesNotThrowAnyException();
    }

    private User saveAndFlushUser(String email, String nickname) {
        return userRepo.saveAndFlush(new User(email, "password", nickname, 16, roleRepo.findByName(RoleName.USER).orElseThrow()));
    }

    private UserIdentity saveAndFlushUserIdentity(User user, AuthProvider provider, String providerUserId) {
        return userIdentityRepo.saveAndFlush(new UserIdentity(user, provider, providerUserId));
    }
}
