package com.example.soso.global.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

public class CookieUtil {

    private static final String SAME_SITE = "None";
    private static final boolean SECURE = true;  // HTTPS 필수 (SameSite=None 요구사항)
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

    public static void deleteRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(HTTP_ONLY)
                .secure(SECURE)
                .path(PATH)
                .maxAge(0)  // 즉시 만료
                .sameSite(SAME_SITE)
                .build();
        response.setHeader("Set-Cookie", cookie.toString());
    }
}
