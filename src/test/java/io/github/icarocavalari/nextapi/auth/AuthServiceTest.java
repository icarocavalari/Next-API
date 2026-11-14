package io.github.icarocavalari.nextapi.auth;

import io.github.icarocavalari.nextapi.auth.token.JwtAccessTokenService;
import io.github.icarocavalari.nextapi.user.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static io.github.icarocavalari.nextapi.user.RoleName.USER;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock
    UserRepository userRepo;

    @Mock
    RoleRepository roleRepo;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    AuthenticationManager authenticationManager;

    @Mock
    JwtAccessTokenService jwtAccessTokenService;

    @InjectMocks
    AuthService authService;

    private final String NORMALIZED_EMAIL = "joaozinho@gmail.com";

    @Test
    void shouldSaveUserWithNormalizedEmailAndEncodedPassword() {
        RegisterRequest registerRequest = createRegisterRequest();
        Role userRole = new Role(USER);
        final String passwordHash = "hashedpassword";

        when(roleRepo.findByName(USER)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(any())).thenReturn(passwordHash);
        when(userRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        authService.registerNewUser(registerRequest);

        verify(userRepo).save(captor.capture());
        User savedUser = captor.getValue();

        assertThat(savedUser.getEmail()).isEqualTo("joaozinho@gmail.com");
        assertThat(savedUser.getNickname()).isEqualTo(registerRequest.nickname());
        assertThat(savedUser.getPassword()).isEqualTo(passwordHash);
        assertThat(savedUser.getRoles().stream().map(Role::getName)).containsExactly(USER);
        assertThat(savedUser.getAge()).isEqualTo(registerRequest.age());

    }

    @Test
    void shouldNotAllowDuplicateEmailsAndNotCreateNewUser() {
        RegisterRequest registerRequest = createRegisterRequest();

        when(userRepo.existsByEmail("joaozinho@gmail.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.registerNewUser(registerRequest))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepo, never()).save(any());
    }

    @Test
    void shouldNotAllowDuplicateNicknameAndNotCreateNewUser() {
        RegisterRequest registerRequest = createRegisterRequest();

        when(userRepo.existsByNicknameIgnoreCase(registerRequest.nickname())).thenReturn(true);

        assertThatThrownBy(() -> authService.registerNewUser(registerRequest))
                .isInstanceOf(NicknameAlreadyExistsException.class);

        verify(userRepo, never()).save(any());
    }

    @Test
    void shouldLoginUserWithNormalizedEmailAndRawPassword() {
        LoginRequest loginRequest = createLoginRequest();
        Role userRole = new Role(USER);

        when(userRepo.findByEmail(NORMALIZED_EMAIL)).thenReturn(Optional.of(
                new User(NORMALIZED_EMAIL, "joaozinho123", "joaozinho", 15, userRole)));

        Instant issuedAt = Instant.parse("2026-01-01T10:00:00Z");

        Jwt jwt = Jwt.withTokenValue("somerandomtoken")
                .header("alg", "HS256")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(900, SECONDS))
                .build();
        when(jwtAccessTokenService.generateToken(any(), any())).thenReturn(jwt);

        ArgumentCaptor<Authentication> captor = ArgumentCaptor.forClass(Authentication.class);

        authService.login(loginRequest);

        verify(authenticationManager).authenticate(captor.capture());
        Authentication authentication = captor.getValue();

        assertThat(authentication.getCredentials()).isEqualTo(loginRequest.password());
        assertThat(authentication.getPrincipal()).isEqualTo(NORMALIZED_EMAIL);

    }

    @Test
    void shouldThrowBadCredentialsWhenWrongCredentialsWereGiven() {
        LoginRequest loginRequest = createLoginRequest();
        when(authenticationManager.authenticate(any())).thenThrow(
                new BadCredentialsException("Wrong Credentials were given"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);

        verify(userRepo, never()).findByEmail(any());
    }

    private RegisterRequest createRegisterRequest() {
        return new RegisterRequest(" Joaozinho@gmail.com ", "joaozinho123", "joAoZinho", 15);
    }

    private LoginRequest createLoginRequest() {
        return new LoginRequest(" Joaozinho@gmail.com ", "joaozinho123");
    }

}
