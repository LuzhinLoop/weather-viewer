package io.controller;

import io.model.entity.User;
import io.service.SessionService;
import io.service.UserService;
import io.web.CookiesUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.nio.file.AccessDeniedException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class GlobalControllerAdvice {

    private final SessionService sessionService;
    private final UserService userService;

    public GlobalControllerAdvice(SessionService sessionService, UserService userService) {
        this.sessionService = sessionService;
        this.userService = userService;
    }

    @ModelAttribute("currentUser")
    public User getCurrentUser(HttpServletRequest request) {
        String sessionTokenString = CookiesUtil.getCookie(request, "session");

        if (sessionTokenString != null) {
            try {
                UUID sessionToken = UUID.fromString(sessionTokenString);
                Optional<Long> userIdOpt = sessionService.getUserIdByValidToken(sessionToken);

                if (userIdOpt.isPresent()) {
                    return userService.findById(userIdOpt.get()).orElse(null);
                }

            } catch (IllegalArgumentException e) {
                log.warn("Invalid UUID format in session cookie");
            }
        }

        return null;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleServerError(Model model) {
        String errorId = UUID.randomUUID().toString();
        log.error("An unexpected server error occurred. Error ID: {}", errorId);

        model.addAttribute("errorTitle", "Server Error");
        model.addAttribute("errorMessage", "An unexpected error occurred on our end. We have been notified.");
        model.addAttribute("errorId", errorId);

        return "error";
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(Model model) {
        log.warn("A non-existent page was requested.");

        model.addAttribute("errorTitle", "Page Not Found");
        model.addAttribute("errorMessage", "Sorry, the page you are looking for does not exist.");

        return "error";
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(Model model) {
        log.warn("Access denied for a user.");

        model.addAttribute("errorTitle", "Access Denied");
        model.addAttribute("errorMessage", "You do not have the necessary permissions to view this page.");

        return "error";
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleValidationErrors(MethodArgumentNotValidException ex, Model model) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.warn("Validation error: {}", errors);

        model.addAttribute("errorTitle", "Validation Error");
        model.addAttribute("errorMessage", "Please check the entered data: " + errors);

        return "error";
    }
}

