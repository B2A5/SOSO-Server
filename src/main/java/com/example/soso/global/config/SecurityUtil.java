package com.example.soso.global.config;

import com.example.soso.security.domain.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 선택적 인증 처리 유틸리티
 *
 * 공개 API에서 로그인 여부에 따라 userId를 선택적으로 추출할 때 사용합니다.
 */
public final class SecurityUtil {

    private SecurityUtil() {}

    /**
     * 현재 인증된 사용자의 ID를 반환합니다.
     * 비인증 요청이면 null을 반환합니다.
     */
    public static String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())
                && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getUser().getId();
        }
        return null;
    }
}
