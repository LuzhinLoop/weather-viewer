package io.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;

@Component
public final class CookiesUtil {

    public static String getCookie(HttpServletRequest req, String name) {
        if (req.getCookies() != null) {
            var hit = Arrays.stream(req.getCookies())
                    .filter(cookie -> name.equals(cookie.getName()))
                    .findFirst();
            if (hit.isPresent()) return hit.get().getValue();
        }

        String header = req.getHeader("Cookie");
        if (header == null) return null;
        for (String part : header.split(";")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length == 2 && name.equals(kv[0])) return kv[1];
        }
        return null;
    }

    public static void setSession(HttpServletResponse response, String token, Duration ttl, boolean secure) {
        ResponseCookie cookie = ResponseCookie.from("session", token)
                .path("/")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .maxAge(ttl)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    public static void deleteCookie(HttpServletResponse response, boolean secure) {
        ResponseCookie cookie = ResponseCookie.from("session", "")
                .path("/")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}
