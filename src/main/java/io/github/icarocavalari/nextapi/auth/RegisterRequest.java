package io.github.icarocavalari.nextapi.auth;

import jakarta.validation.constraints.*;

public record RegisterRequest(@NotBlank @Email String email,
                              @NotBlank @Size(min = 8, max = 60) String password,
                              @NotBlank @Size(min = 3, max = 50) String nickname,
                              @NotNull @Min(12) @Max(100) Integer age) {
}
