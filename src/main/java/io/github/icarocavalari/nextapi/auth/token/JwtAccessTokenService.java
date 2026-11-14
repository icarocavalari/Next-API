package io.github.icarocavalari.nextapi.auth.token;

import io.github.icarocavalari.nextapi.user.Role;
import io.github.icarocavalari.nextapi.user.RoleName;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;


@Service
public class JwtAccessTokenService {

    public static final String ROLES_CLAIM = "roles";
    private final JwtEncoder encoder;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    public JwtAccessTokenService(JwtProperties jwtProperties, Clock clock, JwtEncoder encoder) {
        this.jwtProperties = jwtProperties;
        this.clock = clock;
        this.encoder = encoder;
    }

    public Jwt generateToken(Long id, Set<Role> roles) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(jwtProperties.expiration());

        JwtClaimsSet claimsSet = JwtClaimsSet.builder()
                .subject(id.toString())
                .claim(ROLES_CLAIM, roles.stream()
                        .map(Role::getName)
                        .map(RoleName::name)
                        .toList())
                .issuer(jwtProperties.issuer())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .build();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        JwtEncoderParameters encoderParameters = JwtEncoderParameters.from(header, claimsSet);

        return encoder.encode(encoderParameters);
    }
}
