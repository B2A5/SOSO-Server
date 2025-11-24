package com.example.soso.global.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class CookieUtil {

    @Value("${cookie.secure:true}")
    private boolean secure;

    @Value("${cookie.same-site:None}")
    private String sameSite;

    private static final boolean HTTP_ONLY = true;
    private static final String PATH = "/";

    /**
     * Access Token 쿠키 추가
     * - HttpOnly: true (XSS 공격 방지 - JavaScript 접근 차단)
     * - Secure: 환경별 설정 (로컬: false, 프로덕션: true)
     * - SameSite: 환경별 설정 (로컬: Lax, 프로덕션: None)
     */
    public void addAccessTokenCookie(HttpServletResponse response, String accessToken, long maxAgeMs) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from("accessToken", accessToken)
                .httpOnly(HTTP_ONLY)  // XSS 공격 방지
                .secure(secure)
                .path(PATH)
                .maxAge(Duration.ofMillis(maxAgeMs));

        if (sameSite != null && !sameSite.isEmpty()) {
            builder.sameSite(sameSite);
        }

        response.addHeader("Set-Cookie", builder.build().toString());
    }

    /**
     * Refresh Token 쿠키 추가
     * - HttpOnly: true (XSS 공격 방지)
     * - Secure: 환경별 설정 (로컬: false, 프로덕션: true)
     * - SameSite: 환경별 설정 (로컬: Lax, 프로덕션: None)
     */
    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken, long maxAgeMs) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(HTTP_ONLY)
                .secure(secure)
                .path(PATH)
                .maxAge(Duration.ofMillis(maxAgeMs));

        if (sameSite != null && !sameSite.isEmpty()) {
            builder.sameSite(sameSite);
        }

        response.addHeader("Set-Cookie", builder.build().toString());
    }

    /**
     * Access Token 쿠키 삭제
     */
    public void deleteAccessTokenCookie(HttpServletResponse response) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from("accessToken", "")
                .httpOnly(HTTP_ONLY)
                .secure(secure)
                .path(PATH)
                .maxAge(0);  // 즉시 만료

        if (sameSite != null && !sameSite.isEmpty()) {
            builder.sameSite(sameSite);
        }

        response.addHeader("Set-Cookie", builder.build().toString());
    }

    /**
     * Refresh Token 쿠키 삭제
     */
    public void deleteRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from("refreshToken", "")
                .httpOnly(HTTP_ONLY)
                .secure(secure)
                .path(PATH)
                .maxAge(0);  // 즉시 만료

        if (sameSite != null && !sameSite.isEmpty()) {
            builder.sameSite(sameSite);
        }

        response.addHeader("Set-Cookie", builder.build().toString());
    }
}
