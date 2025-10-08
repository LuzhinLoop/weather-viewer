package io.controller;

import io.model.entity.User;
import io.service.SessionService;
import io.service.UserService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private SessionService sessionService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".html");

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setViewResolvers(viewResolver)
                .build();
    }

    @Test
    void loginForm_WhenUserNotLoggedIn_ShouldReturnLoginPage() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void loginForm_WhenUserLogged_ShouldReturnLoginPage() throws Exception {

        when(sessionService.getUserIdByValidToken(any(UUID.class))).thenReturn(Optional.of(1L));

        mockMvc.perform(get("/auth/login")
                        .cookie(new Cookie("session", "8d8b8719-ea64-4823-91b1-22d515a53c61")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/locations/dashboard"));
    }

    @Test
    void loginForm_WhenUserLoggedIn_ShouldCreateSession() throws Exception {

        User validUser = new User();
        validUser.setId(1L);
        UUID sessionToken = UUID.randomUUID();

        when(userService.login("login", "password")).thenReturn(validUser);
        when(sessionService.createForUser(1L)).thenReturn(sessionToken);

        mockMvc.perform(post("/auth/login")
                .param("login", "login")
                .param("password", "password"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/locations/dashboard"))
                .andExpect(cookie().exists("session"));
    }

    @Test
    void registerForm_whenUserNotAuth_shouldReturnRegisterPage() throws Exception {
        mockMvc.perform(get("/auth/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void registerForm_whenAuthUser_shouldReturnMainPage() throws Exception {

        when(sessionService.getUserIdByValidToken(any(UUID.class)))
                .thenReturn(Optional.of(1L));

        mockMvc.perform(get("/auth/register")
                        .cookie(new Cookie("session", "8d8b8719-ea64-4823-91b1-22d515a53c61")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/locations/dashboard"));

    }

    @Test
    void registerForm_whenUserRegister_shouldReturnMainPage() throws Exception {

        User user = new User();
        user.setId(1L);

        when(userService.registerUser("login", "password", "password"))
                .thenReturn(user);
        when(sessionService.createForUser(1L)).thenReturn(UUID.randomUUID());

        mockMvc.perform(post("/auth/register")
                .param("login", "login")
                .param("password", "password")
                .param("confirmPassword", "password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/locations/dashboard"));

    }

    @Test
    void registerForm_whenLoginIsInvalid_shouldExpectedReturnLoginPage() throws Exception {

        mockMvc.perform(post("/auth/register")
                .param("login", "")
                .param("password", "password123")
                .param("confirmPassword", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/register"));
    }

    @Test
    void logout_WhenUserIsLoggedIn_ShouldRedirectAndClearCookie() throws Exception {

        when(sessionService.getUserIdByValidToken(any(UUID.class))).thenReturn(Optional.of(1L));

        mockMvc.perform(post("/auth/logout")
                .cookie(new Cookie("session", "8d8b8719-ea64-4823-91b1-22d515a53c61")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));

    }
}

