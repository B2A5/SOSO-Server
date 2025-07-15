package com.example.soso.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Order(0)
@Component
public class ExceptionHandlerFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            log.info("AccessToken 만료됨");
            setErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "ACCESS_TOKEN_EXPIRED");
        } catch (JwtException e) {
            log.warn("JWT 예외 발생: {}", e.getMessage());
            setErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "INVALID_TOKEN");

        } catch (AuthenticationException e) {
            log.warn("인증 예외 발생: {}", e.getMessage());
            setErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "인증에 실패하였습니다.");

        } catch (Exception e) {
            log.error("서버 내부 오류 발생: {}", e.getMessage(), e);
            setErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생하였습니다.");
        }
    }

    private void setErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json; charset=UTF-8");

        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("code", status);
        errorBody.put("message", message);

        objectMapper.writeValue(response.getWriter(), errorBody);
    }
}
