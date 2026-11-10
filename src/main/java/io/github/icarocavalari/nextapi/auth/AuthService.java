package io.github.icarocavalari.nextapi.auth;

import io.github.icarocavalari.nextapi.user.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;


@Service
public class AuthService {
    private final UserRepository userRepo;

    private final RoleRepository roleRepo;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepo, RoleRepository roleRepo, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
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

    public UserResponse login(LoginRequest loginRequest) {
        String normalizedEmail = normalizeEmail(loginRequest.email());

        authenticationManager.authenticate(UsernamePasswordAuthenticationToken
                        .unauthenticated(normalizedEmail, loginRequest.password()));

        User user = userRepo.findByEmail(normalizedEmail).orElseThrow();

        return UserResponse.from(user);
    }

    private String normalizeEmail(String email) {
        return email.toLowerCase(Locale.ROOT).trim();
    }
}
