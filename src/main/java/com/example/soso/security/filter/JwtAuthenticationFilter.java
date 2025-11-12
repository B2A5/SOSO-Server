package com.example.soso.security.filter;

import com.example.soso.global.jwt.JwtProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Order(1)
@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String token = resolveToken(request);

            if (StringUtils.hasText(token)) {
                if (jwtProvider.validateToken(token)) {
                    // 유효한 토큰이면 인증 정보 설정
                    setAuthentication(token);
                } else {
                    // 토큰이 있지만 유효하지 않으면 로그만 남기고 Spring Security가 처리하도록 함
                    log.warn("유효하지 않은 JWT 토큰: {}", request.getRequestURI());
                    SecurityContextHolder.clearContext();
                }
            }
            // 토큰이 없으면 아무것도 하지 않고, Spring Security의 authenticationEntryPoint가 처리

        } catch (Exception e) {
            log.error("JWT 처리 중 오류 발생: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * HTTP 요청에서 JWT 토큰 추출
     *
     * 우선순위:
     * 1. Authorization 헤더 (기존 호환성 유지)
     * 2. accessToken 쿠키 (httpOnly=true로 XSS 방어)
     */
    private String resolveToken(HttpServletRequest request) {
        // 1순위: Authorization 헤더에서 추출 (하위 호환성)
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // 2순위: 쿠키에서 추출 (httpOnly=true)
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    String token = cookie.getValue();
                    if (StringUtils.hasText(token)) {
                        log.debug("쿠키에서 AccessToken 추출: {}", request.getRequestURI());
                        return token;
                    }
                }
            }
        }

        return null;
    }

    /**
     * JWT 토큰으로부터 인증 정보 설정
     */
    private void setAuthentication(String token) {
        try {
            String userId = jwtProvider.getSubject(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(userId);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("JWT 인증 성공: {}", userId);

        } catch (UsernameNotFoundException e) {
            log.warn("사용자를 찾을 수 없음: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        } catch (Exception e) {
            log.error("인증 설정 중 오류: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }
    }
}
