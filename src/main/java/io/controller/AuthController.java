package io.controller;

import io.model.command.LoginCommand;
import io.model.command.RegisterRequest;
import io.model.entity.User;
import io.service.SessionService;
import io.service.UserService;
import io.web.CookiesUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;


@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class    AuthController {

    private final UserService userService;
    private final SessionService sessionService;

    @Value("${server.ssl.enabled:false}")
    private boolean sslEnabled;

    @Value("${server.session.ttl-hours}")
    private long sessionTtlHours;

    @GetMapping("/login")
    public String loginForm(HttpServletRequest req, Model model) {
        String sessionToken = CookiesUtil.getCookie(req, "session");

        if (findValidSessionToken(sessionToken).isPresent()) {
            return "redirect:/locations/dashboard";
        }

        model.addAttribute("loginCommand", new LoginCommand("", ""));
        return "login";
    }

    @PostMapping("/login")
    public String loginSubmit(@Valid @ModelAttribute LoginCommand command,
                              BindingResult bindingResult,
                              HttpServletResponse response,
                              Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("loginCommand", command);
            return "login";
        }

        try {
            User user = userService.login(command.login(), command.password());

            UUID sessionToken = sessionService.createForUser(user.getId());

            CookiesUtil.setSession(
                    response,
                    sessionToken.toString(),
                    Duration.ofHours(sessionTtlHours),
                    sslEnabled
            );

            return "redirect:/locations/dashboard";
        } catch (IllegalArgumentException e) {
            model.addAttribute("loginCommand", command);
            model.addAttribute("errorMessage", "Invalid username or password.");
            return "login";
        }
    }

    @GetMapping("/register")
    public String registerForm(HttpServletRequest req, Model model) {
        String token = CookiesUtil.getCookie(req, "session");

        if (token != null && !token.isBlank()) {
            try {
                UUID uuid = UUID.fromString(token);
                if (sessionService.getUserIdByValidToken(uuid).isPresent()) {
                    return "redirect:/locations/dashboard";
                }
            } catch (IllegalArgumentException ignored) { /* невалидный UUID -> гость */ }
        }

        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest("", "", ""));
        }
        model.addAttribute("redirectTo", "/");
        return "register";
    }

    @PostMapping("/register")
    public String registerSubmit(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                                 BindingResult binding,
                                 HttpServletResponse response,
                                 RedirectAttributes ra) {

        if (binding.hasErrors()) {
            ra.addFlashAttribute("registerRequest", request);
            ra.addFlashAttribute("org.springframework.validation.BindingResult.registerRequest", binding);
            return "redirect:/auth/register";
        }

        try {
            User newUser = userService.registerUser(request.login().trim(), request.password(), request.confirmPassword());

            UUID sessionToken = sessionService.createForUser(newUser.getId());
            CookiesUtil.setSession(
                    response,
                    sessionToken.toString(),
                    Duration.ofHours(sessionTtlHours),
                    sslEnabled
            );

            ra.addFlashAttribute("successMessage", "Welcome! Your registration was successful!");
            return "redirect:/locations/dashboard";

        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("registerRequest", request);
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/auth/register";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request,
                         HttpServletResponse response,
                         RedirectAttributes redirectAttributes) {

        String sessionToken = CookiesUtil.getCookie(request, "session");

        findValidSessionToken(sessionToken).ifPresent(sessionService::logout);

        CookiesUtil.deleteCookie(response, sslEnabled);

        redirectAttributes.addFlashAttribute("logoutMessage", "You have successfully logged out.");
        return "redirect:/auth/login";
    }

    private Optional<UUID> findValidSessionToken(String sessionToken) {
        if (sessionToken == null) {
            return Optional.empty();
        }
        try {
            UUID token = UUID.fromString(sessionToken);
            boolean isValid = sessionService.getUserIdByValidToken(token).isPresent();
            return isValid ? Optional.of(token) : Optional.empty();
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
