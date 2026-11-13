package io.github.icarocavalari.nextapi.auth;

import io.github.icarocavalari.nextapi.config.SecurityConfig;
import io.github.icarocavalari.nextapi.user.UserResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ExtendWith(MockitoExtension.class)
@Import(SecurityConfig.class)
public class AuthControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AuthService authService;

    private final String JSON_REGISTER_REQUEST = """
            {
                "email": "joaozinho@gmail.com",
                "password": "joaozinho123",
                "nickname": "joaozinho",
                "age": 15
            }""";

    private final String JSON_LOGIN_REQUEST = """
            {
                "email": "joaozinho@gmail.com",
                "password": "joaozinho123"
            }""";

    @Test
    void shouldReturnStatusCreatedWhenCorrectFieldsWereGiven() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "joaozinho@gmail.com", "joaozinho123", "joaozinho", 15);

        when(authService.registerNewUser(registerRequest)).thenReturn(
                new UserResponse(15L, "joaozinho@gmail.com", "joaozinho", 15));

        mockMvc.perform(post("/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(JSON_REGISTER_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("joaozinho@gmail.com"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.id").value(15L))
                .andExpect(jsonPath("$.nickname").value("joaozinho"))
                .andExpect(jsonPath("$.age").value(15));

    }

    @Test
    void shouldValidateEmptyFieldOnTheBodyRequest() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(APPLICATION_JSON)
                .content("""
                                {
                                    "email": "",
                                    "password": "joaozinho123",
                                    "nickname": "joaozinho",
                                    "age": 15
                                }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());

        verify(authService, never()).registerNewUser(any());
    }

    @Test
    void shouldReturnConflictWithEmailDuplicity() throws Exception {
        when(authService.registerNewUser(any())).thenThrow(
                new EmailAlreadyExistsException("Email is already being used"));

        mockMvc.perform(post("/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(JSON_REGISTER_REQUEST))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Email is already being used"));
    }
    
    @Test
    void shouldReturnConflictWithNicknameDuplicity() throws Exception {
        when(authService.registerNewUser(any())).thenThrow(
                new NicknameAlreadyExistsException("Nickname is already being used"));

        mockMvc.perform(post("/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(JSON_REGISTER_REQUEST))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Nickname is already being used"));
    }

    @Test
    void shouldReturnConflictWhenDuplicityReachesDatabase() throws Exception {
        when(authService.registerNewUser(any())).thenThrow(
                new DataIntegrityViolationException("uk_users_email"));

        mockMvc.perform(post("/auth/register")
                .contentType(APPLICATION_JSON)
                .content(JSON_REGISTER_REQUEST))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Email is already being used"));
    }

    @Test
    void shouldReturnInternalServerErrorWhenDataIntegrityAndIsNotDuplicity() throws Exception {
        when(authService.registerNewUser(any())).thenThrow(
                new DataIntegrityViolationException(""));

        mockMvc.perform(post("/auth/register")
                .contentType(APPLICATION_JSON)
                .content(JSON_REGISTER_REQUEST))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"));
    }

    @Test
    void shouldReturnOkWhenLoggingUserCorrectly() throws Exception {
        LoginRequest loginRequest = new LoginRequest("joaozinho@gmail.com", "joaozinho123");

        when(authService.login(loginRequest)).thenReturn(
                new UserResponse(15L, "joaozinho@gmail.com", "joaozinho", 15));

        mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(JSON_LOGIN_REQUEST))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("joaozinho@gmail.com"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.id").value(15L))
                .andExpect(jsonPath("$.nickname").value("joaozinho"))
                .andExpect(jsonPath("$.age").value(15));
    }

    @Test
    void shouldReturnUnauthorizedWhenUserWasNotAuthenticated() throws Exception {
        when(authService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(JSON_LOGIN_REQUEST))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Email/Password is incorrect."));
    }

    @Test
    void shouldReturnBadRequestWhenFieldIsBlankInRequestBody() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "",
                                    "password": "joaozinho123"
                                }"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());

        verify(authService, never()).login(any());
    }
}
