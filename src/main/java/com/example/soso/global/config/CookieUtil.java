package com.example.soso.global.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

public class CookieUtil {

    private static final String SAME_SITE = "None";
    private static final boolean SECURE = true;  // HTTPS 필수 (SameSite=None 요구사항)
    private static final boolean HTTP_ONLY = true;
    private static final String PATH = "/";

    /**
     * Access Token 쿠키 추가
     * - HttpOnly: true (XSS 공격 방지 - JavaScript 접근 차단)
     * - Secure: true (HTTPS 전용)
     * - SameSite: None (크로스 도메인 요청 허용)
     */
    public static void addAccessTokenCookie(HttpServletResponse response, String accessToken, long maxAgeMs) {
        ResponseCookie cookie = ResponseCookie.from("accessToken", accessToken)
                .httpOnly(HTTP_ONLY)  // XSS 공격 방지
                .secure(SECURE)
                .path(PATH)
                .maxAge(Duration.ofMillis(maxAgeMs))
                .sameSite(SAME_SITE)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    /**
     * Refresh Token 쿠키 추가
     * - HttpOnly: true (XSS 공격 방지)
     * - Secure: true (HTTPS 전용)
     * - SameSite: None (크로스 도메인 요청 허용)
     */
    public static void addRefreshTokenCookie(HttpServletResponse response, String refreshToken, long maxAgeMs) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(HTTP_ONLY)
                .secure(SECURE)
                .path(PATH)
                .maxAge(Duration.ofMillis(maxAgeMs))
                .sameSite(SAME_SITE)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    /**
     * Access Token 쿠키 삭제
     */
    public static void deleteAccessTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("accessToken", "")
                .httpOnly(HTTP_ONLY)
                .secure(SECURE)
                .path(PATH)
                .maxAge(0)  // 즉시 만료
                .sameSite(SAME_SITE)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    /**
     * Refresh Token 쿠키 삭제
     */
    public static void deleteRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(HTTP_ONLY)
                .secure(SECURE)
                .path(PATH)
                .maxAge(0)  // 즉시 만료
                .sameSite(SAME_SITE)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}
