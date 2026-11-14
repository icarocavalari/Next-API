package io.github.icarocavalari.nextapi.auth;

import io.github.icarocavalari.nextapi.auth.token.JwtAccessTokenService;
import io.github.icarocavalari.nextapi.auth.token.TokenResponse;
import io.github.icarocavalari.nextapi.user.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Locale;


@Service
public class AuthService {
    private final UserRepository userRepo;

    private final RoleRepository roleRepo;

    private final JwtAccessTokenService jwtAccessTokenService;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepo, RoleRepository roleRepo, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtAccessTokenService jwtAccessTokenService) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtAccessTokenService = jwtAccessTokenService;
    }

    @Transactional
    public UserResponse registerNewUser(RegisterRequest registerRequest) {
        String normalizedEmail = normalizeEmail(registerRequest.email());

        if (userRepo.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException("Email is already being used");
        }

        String normalizedNickname = registerRequest.nickname().trim();

        if (userRepo.existsByNicknameIgnoreCase(normalizedNickname)) {
            throw new NicknameAlreadyExistsException("Nickname is already being used");
        }

        Role userRole = roleRepo.findByName(RoleName.USER).orElseThrow(() ->
                new IllegalStateException("Role could not be found"));

        String encodedPassword = passwordEncoder.encode(registerRequest.password());

        User savedUser = userRepo.save(new User(normalizedEmail, encodedPassword,
                                        normalizedNickname, registerRequest.age(), userRole));

        return UserResponse.from(savedUser);
    }

    public TokenResponse login(LoginRequest loginRequest) {
        String normalizedEmail = normalizeEmail(loginRequest.email());

        authenticationManager.authenticate(UsernamePasswordAuthenticationToken
                        .unauthenticated(normalizedEmail, loginRequest.password()));

        User user = userRepo.findByEmail(normalizedEmail).orElseThrow(() ->
                new IllegalStateException("Could not find user by the email"));

        Jwt jwt = jwtAccessTokenService.generateToken(user.getId(), user.getRoles());
        long expiresIn = Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()).getSeconds();

        return TokenResponse.bearer(jwt.getTokenValue(), expiresIn);
    }

    private String normalizeEmail(String email) {
        return email.toLowerCase(Locale.ROOT).trim();
    }
}
