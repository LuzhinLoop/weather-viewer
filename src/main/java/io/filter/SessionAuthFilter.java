package io.filter;

import io.service.SessionService;
import io.web.CookiesUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SessionAuthFilter extends OncePerRequestFilter {

    private final SessionService sessionService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/auth/") ||
                path.startsWith("/assets/") ||
                path.startsWith("/css/") ||
                path.startsWith("/js/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        String sessionCookie = CookiesUtil.getCookie(req, "session");

        if (sessionCookie != null) {
            try {
                UUID token = UUID.fromString(sessionCookie);
                Optional<Long> userIdOpt = sessionService.getUserIdByValidToken(token);

                if (userIdOpt.isPresent()) {
                    req.setAttribute("userId", userIdOpt.get());
                    chain.doFilter(req, res);
                    return;
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        String originalUri = req.getRequestURI() + (req.getQueryString() != null ? "?" + req.getQueryString() : "");
        String redirectUrl = "/auth/login?redirect=" + URLEncoder.encode(originalUri, StandardCharsets.UTF_8);
        res.sendRedirect(redirectUrl);
    }
}