package io.github.icarocavalari.nextapi.auth;

import io.github.icarocavalari.nextapi.user.Role;
import io.github.icarocavalari.nextapi.user.User;
import io.github.icarocavalari.nextapi.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static io.github.icarocavalari.nextapi.user.RoleName.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserDetailsServiceImplTest {
    @Mock
    UserRepository userRepo;

    @InjectMocks
    UserDetailsServiceImpl userDetailsService;

    @Test
    void shouldReturnUserDetailsCorrectData() {
        User user = createUser();

        when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

        assertThat(userDetails.getUsername()).isEqualTo(user.getEmail());
        assertThat(userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority))
                .containsExactly("ROLE_USER");
        assertThat(userDetails.getPassword()).isEqualTo(user.getPassword());
    }

    @Test
    void shouldThrowUsernameNotFoundWhenUserWasNotFound() {
        String email = "joaozinho@gmail.com";
        when(userRepo.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(email))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void shouldThrowUsernameNotFoundWhenPasswordIsNull() {
        User user = new User("joaozinho@gmail.com", null, "joaozinho", 15, new Role(USER));

        when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(user.getEmail()))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    private User createUser() {
        return new User(
                "joaozinho@gmail.com", "joaozinho123", "joaozinho", 15, new Role(USER));
    }
}
