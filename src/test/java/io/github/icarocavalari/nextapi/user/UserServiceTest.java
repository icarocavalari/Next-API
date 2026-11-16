package io.github.icarocavalari.nextapi.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static io.github.icarocavalari.nextapi.user.RoleName.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    UserRepository userRepo;

    @InjectMocks
    UserService userService;

    @Test
    void shouldReturnUserResponseWithCorrectInfoFromTheUser() {
        User user = createUser();
        ReflectionTestUtils.setField(user,"id", 15L);

        when(userRepo.findById(user.getId())).thenReturn(Optional.of(user));

        UserResponse userResponse = userService.getById(user.getId());

        assertThat(userResponse.id()).isEqualTo(user.getId());
        assertThat(userResponse.email()).isEqualTo(user.getEmail());
        assertThat(userResponse.nickname()).isEqualTo(user.getNickname());
        assertThat(userResponse.age()).isEqualTo(user.getAge());
    }

    @Test
    void shouldThrowUserNotFoundExceptionWhenUserRepoCouldNotFindUser() {
        Long id = 15L;

        when(userRepo.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(id))
                .isInstanceOf(UserNotFoundException.class);
    }

    private User createUser() {
        Role userRole = new Role(USER);
        return new User("joaozinho@gmail.com", "joaozinho123", "joaozinho", 15, userRole);
    }

}
