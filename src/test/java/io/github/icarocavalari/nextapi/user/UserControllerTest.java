package io.github.icarocavalari.nextapi.user;

import io.github.icarocavalari.nextapi.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
public class UserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserService userService;

    @MockitoBean
    JwtAuthenticationConverter jwtAuthenticationConverter;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Test
    void shouldReturnOkAndCorrectJsonDataFromUserResponse() throws Exception {
        UserResponse userResponse = new UserResponse(15L, "joaozinho@gmail.com", "joaozinho", 15);

        when(userService.getById(userResponse.id())).thenReturn(userResponse);

        mockMvc.perform(get("/users/me")
                .with(jwt()
                        .jwt(token -> token
                                .subject(userResponse.id().toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(15L))
                .andExpect(jsonPath("$.email").value("joaozinho@gmail.com"))
                .andExpect(jsonPath("$.nickname").value("joaozinho"))
                .andExpect(jsonPath("$.age").value(15));
    }

    @Test
    void shouldReturnNotFoundWhenUserNotFoundExceptionWasThrown() throws Exception {
        Long id = 15L;
        String errorMessage = "User not found";
        when(userService.getById(id)).thenThrow(new UserNotFoundException(errorMessage));

        mockMvc.perform(get("/users/me")
                    .with(jwt()
                            .jwt(token -> token
                                    .subject(id.toString()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail")
                        .value(errorMessage));
    }

    @Test
    void shouldReturnUnauthorizedWhenRequestDoesNotHaveToken() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail")
                        .value("Authentication is required to access this resource"));
    }
}
