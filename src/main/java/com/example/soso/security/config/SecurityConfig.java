package com.example.soso.security.config;


import com.example.soso.global.jwt.JwtProvider;
import com.example.soso.security.filter.ExceptionHandlerFilter;
import com.example.soso.security.filter.JwtAuthenticationFilter;
import com.example.soso.security.handler.JwtAccessDeniedHandler;
import com.example.soso.security.handler.JwtAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final UserDetailsService userDetailsService;
    private final CorsConfigurationSource corsConfigurationSource;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)      // form login 사용 안 함
                .httpBasic(AbstractHttpConfigurer::disable)      // 브라우저 팝업 로그인 끔
                .logout(AbstractHttpConfigurer::disable)       //  로그아웃 처리도 직접 API로 할 것
                .rememberMe(AbstractHttpConfigurer::disable) // 자동 로그인 X
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // 세션 완전 끔
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        // CORS preflight 요청 허용 (모든 OPTIONS 요청)
                        .requestMatchers("OPTIONS", "/**").permitAll()
                        // 인증 없이 접근 가능한 경로들
                        .requestMatchers(
                                "/auth/**",           // 인증 관련
                                "/login/**",          // 로그인
                                "/signup/**",         // 회원가입
                                "/swagger-ui/**",     // API 문서
                                "/v3/api-docs/**",    // API 문서
                                "/kafka/**",          // 카프카
                                "/actuator/health"    // 헬스체크 (Docker healthcheck 및 모니터링용)
                        ).permitAll()
                        // 자유게시판 조회 (인증 불필요)
                        .requestMatchers("GET", "/community/freeboard/**").permitAll()
                        // 그 외 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )
                .with(JwtSecurityDsl.customDsl(), dsl -> dsl.enable(true));
        return http.build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtProvider, userDetailsService);
    }

    @Bean
    public ExceptionHandlerFilter exceptionHandlerFilter() {
        return new ExceptionHandlerFilter();
    }


    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        return http.getSharedObject(AuthenticationManager.class);
    }
}
