package com.example.soso.global.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

public class CookieUtil {

    private static final String SAME_SITE = "None";
    private static final boolean SECURE = false;
    private static final boolean HTTP_ONLY = true;
    private static final String PATH = "/";

    public static void addRefreshTokenCookie(HttpServletResponse response, String refreshToken, long maxAgeMs) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(HTTP_ONLY)
                .secure(SECURE)
                .path(PATH)
                .maxAge(Duration.ofMillis(maxAgeMs))
                .sameSite(SAME_SITE)
                .build();
        response.setHeader("Set-Cookie", cookie.toString());
    }
}
