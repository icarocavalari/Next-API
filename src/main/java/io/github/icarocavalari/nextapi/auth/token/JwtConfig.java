package io.github.icarocavalari.nextapi.auth.token;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
public class JwtConfig {

    private static final int MIN_KEY_BYTES = 32;

    private final SecretKey secretKey;

    private final String issuer;

    public JwtConfig(JwtProperties jwtProperties) {
        byte[] keyBytes = decodeSecret(jwtProperties.secret());
        if (keyBytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET_KEY must be at least " + MIN_KEY_BYTES + " bytes after Base64 decoding");
        }

        this.secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        this.issuer = jwtProperties.issuer();
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return NimbusJwtEncoder
                .withSecretKey(secretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }

    private static byte[] decodeSecret(String key) {
        try {
            return Base64.getDecoder().decode(key);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("JWT_SECRET_KEY was not given in a valid Base64 String", e);
        }
    }
}
