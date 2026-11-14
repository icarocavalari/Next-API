package io.github.icarocavalari.nextapi.auth.token;

import io.github.icarocavalari.nextapi.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import static io.github.icarocavalari.nextapi.user.RoleName.ADMIN;
import static io.github.icarocavalari.nextapi.user.RoleName.USER;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class JwtAccessTokenServiceTest {
    private static final JwtProperties JWT_PROPERTIES = new JwtProperties(
            "somerandomtoken", "next-api", Duration.of(15, MINUTES));

    @Mock
    JwtEncoder encoder;

    Clock clock;

    JwtAccessTokenService jwtAccessTokenService;

    @BeforeEach
    void createService() {
        Instant now = Instant.parse("2026-11-14T10:00:00Z");
        this.clock = Clock.fixed(now, ZoneOffset.UTC);
        this.jwtAccessTokenService = new JwtAccessTokenService(JWT_PROPERTIES, clock, encoder);
    }

    @Test
    void shouldGenerateTokenCorrectlyWithCorrectUserAndRoles() {
        Long id = 15L;
        Role userRole = new Role(USER);
        Role adminRole = new Role(ADMIN);
        Set<Role> roles = Set.of(userRole, adminRole);
        Instant now = clock.instant();

        ArgumentCaptor<JwtEncoderParameters> captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);

        jwtAccessTokenService.generateToken(id, roles);
        verify(encoder).encode(captor.capture());
        JwtEncoderParameters encoderParameters = captor.getValue();

        JwsHeader jwsHeader = encoderParameters.getJwsHeader();
        JwtClaimsSet claimsSet = encoderParameters.getClaims();

        assertThat(jwsHeader.getAlgorithm()).isEqualTo(MacAlgorithm.HS256);
        assertThat(claimsSet.getSubject()).isEqualTo(id.toString());
        assertThat(claimsSet.getClaimAsStringList("roles")).containsExactlyInAnyOrder("USER", "ADMIN");
        assertThat(claimsSet.getClaimAsString("iss")).isEqualTo(JWT_PROPERTIES.issuer());
        assertThat(claimsSet.getIssuedAt()).isEqualTo(now);
        assertThat(claimsSet.getExpiresAt()).isEqualTo(now.plus(JWT_PROPERTIES.expiration()));
    }
}
