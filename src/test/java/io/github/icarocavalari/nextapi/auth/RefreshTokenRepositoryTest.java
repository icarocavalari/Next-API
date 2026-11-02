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

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
public class RefreshTokenRepositoryTest {
    @Autowired
    RefreshTokenRepository refreshTokenRepo;
    @Autowired
    UserRepository userRepo;
    @Autowired
    RoleRepository roleRepo;
    @Autowired
    TestEntityManager testEntityManager;

    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.MINUTES);
    private static final String TOKEN_HASH = "123456789";
    private static final Instant EXPIRE_AT = NOW.plus(Duration.ofDays(7));

    @Test
    void shouldReturnCorrectTokenAfterSaving() {
        User user = saveAndFlushUser("test@example.com", "nickname");
        RefreshToken saved = saveAndFlushToken(TOKEN_HASH, user, EXPIRE_AT);
        testEntityManager.clear();

        RefreshToken found = refreshTokenRepo.findByTokenHash(TOKEN_HASH).orElseThrow();

        assertThat(found.getId()).isEqualTo(saved.getId());
    }

    @Test
    void shouldNotReturnTokenWithInvalidHash() {
        assertThat(refreshTokenRepo.findByTokenHash("invalidtokenhash")).isEmpty();
    }

    @Test
    void shouldNotCreateTwoTokensWithSameHash() {
        User first = saveAndFlushUser("first@example.com", "first");
        User second = saveAndFlushUser("second@example.com", "second");

        saveAndFlushToken(TOKEN_HASH, first, EXPIRE_AT);

        assertThatThrownBy(() -> {
            saveAndFlushToken(TOKEN_HASH, second, EXPIRE_AT);
        }).isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("refresh_tokens_token_hash_key");
    }

    @Test
    void shouldRevokeActiveTokensOfAGivenUser() {
        User first = saveAndFlushUser("first@example.com", "first");
        User second = saveAndFlushUser("second@example.com", "second");

        RefreshToken tokenOneA = saveAndFlushToken(TOKEN_HASH, first, EXPIRE_AT);
        RefreshToken tokenTwoA = saveAndFlushToken("987654321", first, EXPIRE_AT);

        RefreshToken tokenRevokedA = saveAndFlushToken("123454321", first, EXPIRE_AT);
        tokenRevokedA.revoke(NOW.minus(Duration.ofHours(1)));
        testEntityManager.flush();

        RefreshToken tokenOneB = saveAndFlushToken("987656789", second, EXPIRE_AT);

        assertThat(refreshTokenRepo.revokeAllTokensFromUser(first.getId(), NOW)).isEqualTo(2);

        RefreshToken foundTokenOneA = refreshTokenRepo.findById(tokenOneA.getId()).orElseThrow();
        RefreshToken foundTokenTwoA = refreshTokenRepo.findById(tokenTwoA.getId()).orElseThrow();
        RefreshToken foundTokenRevokedA = refreshTokenRepo.findById(tokenRevokedA.getId()).orElseThrow();
        RefreshToken foundTokenOneB = refreshTokenRepo.findById(tokenOneB.getId()).orElseThrow();

        assertThat(foundTokenOneA.getRevokedAt()).isEqualTo(NOW);
        assertThat(foundTokenTwoA.getRevokedAt()).isEqualTo(NOW);
        assertThat(foundTokenRevokedA.getRevokedAt()).isEqualTo(NOW.minus(Duration.ofHours(1)));
        assertThat(foundTokenOneB.getRevokedAt()).isNull();
    }

    private User saveAndFlushUser(String email, String nickname) {
        return userRepo.saveAndFlush(new User(email, "password", nickname, 16, roleRepo.findByName(RoleName.USER).orElseThrow()));
    }

    private RefreshToken saveAndFlushToken(String tokenHash, User user, Instant expireAt) {
        return refreshTokenRepo.saveAndFlush(new RefreshToken(tokenHash, user, expireAt));
    }
}