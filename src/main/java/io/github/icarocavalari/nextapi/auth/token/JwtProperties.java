package io.github.icarocavalari.nextapi.auth.token;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "jwt")
@Validated
public record JwtProperties(@NotBlank String secret, @NotBlank String issuer, @NotNull Duration expiration) {}
