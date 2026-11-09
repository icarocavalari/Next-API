package io.github.icarocavalari.nextapi.auth;

import io.github.icarocavalari.nextapi.user.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class AuthService {
    private final UserRepository userRepo;

    private final RoleRepository roleRepo;

    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepo, RoleRepository roleRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse registerNewUser(RegisterRequest registerRequest) {
        String normalizedEmail = registerRequest.email().toLowerCase().trim();

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
}
