package com.example.soso.users.integration;

import com.example.soso.config.TestRedisConfig;
import com.example.soso.global.jwt.JwtProvider;
import com.example.soso.global.redis.RefreshTokenRedisRepository;
import com.example.soso.users.domain.entity.UserType;
import com.example.soso.users.domain.entity.Users;
import com.example.soso.users.repository.UsersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.Cookie;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 인증 시스템 쿠키 통합 테스트
 *
 * 테스트 목표:
 * - Access Token이 응답 Body와 쿠키 모두에 포함되는지 확인
 * - Refresh Token이 쿠키에 포함되는지 확인
 * - 로그아웃 시 쿠키가 삭제되는지 확인
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestRedisConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.session.store-type=none",
        "jwt.secret-key=test-jwt-secret-key-that-is-sufficiently-long-and-secure-for-testing-purposes-minimum-256-bits-required-by-jwt-library",
        "jwt.access-token-validity-in-ms=1800000",
        "jwt.refresh-token-validity-in-ms=1209600000",
        "cookie.secure=true",
        "cookie.same-site=None"
})
@AutoConfigureMockMvc
@Transactional
@DisplayName("인증 시스템 쿠키 통합 테스트")
class AuthCookieIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private RefreshTokenRedisRepository refreshTokenRedisRepository;

    private Users testUser;

    @BeforeEach
    void setUp() {
        // 테스트 유저 생성
        testUser = Users.builder()
                .username("쿠키테스트유저_" + System.currentTimeMillis())
                .nickname("쿠키테스터")
                .userType(UserType.FOUNDER)
                .email("cookie_test_" + System.currentTimeMillis() + "@example.com")
                .location("11680")
                .build();
        testUser = usersRepository.save(testUser);
    }

    @Test
    @DisplayName("토큰 재발급 시 Access Token이 Body와 쿠키 모두에 포함됨")
    void refreshToken_ReturnsAccessTokenInBodyAndCookie() throws Exception {
        // given - Refresh Token 생성 및 Redis 저장
        String refreshToken = jwtProvider.generateRefreshToken();
        refreshTokenRedisRepository.save(refreshToken, testUser.getId(), 7 * 24 * 60 * 60 * 1000L);

        // when & then
        mockMvc.perform(post("/auth/refresh")
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                // Body에 accessToken 포함 확인
                .andExpect(jsonPath("$.jwtAccessToken").isNotEmpty())
                // Set-Cookie 헤더에 accessToken 쿠키 포함 확인
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(result -> {
                    var setCookieHeaders = result.getResponse().getHeaders("Set-Cookie");
                    assert !setCookieHeaders.isEmpty();

                    boolean hasAccessToken = false;
                    boolean hasSecure = false;
                    boolean hasSameSite = false;

                    for (String cookie : setCookieHeaders) {
                        if (cookie.contains("accessToken=")) {
                            hasAccessToken = true;
                            if (cookie.contains("Secure")) hasSecure = true;
                            if (cookie.contains("SameSite=None")) hasSameSite = true;
                        }
                    }

                    assert hasAccessToken : "accessToken 쿠키가 없습니다";
                    assert hasSecure : "Secure 속성이 없습니다";
                    assert hasSameSite : "SameSite=None 속성이 없습니다";
                });
    }

    @Test
    @DisplayName("토큰 재발급 시 Refresh Token 쿠키도 갱신됨")
    void refreshToken_UpdatesRefreshTokenCookie() throws Exception {
        // given
        String refreshToken = jwtProvider.generateRefreshToken();
        refreshTokenRedisRepository.save(refreshToken, testUser.getId(), 7 * 24 * 60 * 60 * 1000L);

        // when & then
        mockMvc.perform(post("/auth/refresh")
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String setCookieHeaders = String.join(", ", result.getResponse().getHeaders("Set-Cookie"));
                    // refreshToken 쿠키 포함 확인
                    assert setCookieHeaders.contains("refreshToken=");
                    // HttpOnly 확인
                    assert setCookieHeaders.contains("HttpOnly");
                    // Secure 확인
                    assert setCookieHeaders.contains("Secure");
                });
    }

    @Test
    @DisplayName("로그아웃 시 Access Token과 Refresh Token 쿠키가 삭제됨")
    void logout_DeletesAllTokenCookies() throws Exception {
        // given
        String refreshToken = jwtProvider.generateRefreshToken();
        refreshTokenRedisRepository.save(refreshToken, testUser.getId(), 7 * 24 * 60 * 60 * 1000L);

        // when & then
        mockMvc.perform(post("/auth/logout")
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String setCookieHeaders = String.join(", ", result.getResponse().getHeaders("Set-Cookie"));
                    // accessToken 쿠키 삭제 확인 (Max-Age=0)
                    assert setCookieHeaders.contains("accessToken=");
                    assert setCookieHeaders.contains("Max-Age=0");
                    // refreshToken 쿠키 삭제 확인
                    assert setCookieHeaders.contains("refreshToken=");
                });
    }

    @Test
    @DisplayName("Access Token 쿠키는 HttpOnly=true (XSS 방어)")
    void accessTokenCookie_IsHttpOnly() throws Exception {
        // given
        String refreshToken = jwtProvider.generateRefreshToken();
        refreshTokenRedisRepository.save(refreshToken, testUser.getId(), 7 * 24 * 60 * 60 * 1000L);

        // when & then
        mockMvc.perform(post("/auth/refresh")
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    var setCookieHeaders = result.getResponse().getHeaders("Set-Cookie");
                    boolean foundAccessTokenWithHttpOnly = false;

                    // accessToken 쿠키 찾기
                    for (String cookie : setCookieHeaders) {
                        if (cookie.contains("accessToken=")) {
                            // HttpOnly가 있어야 함 (XSS 방어)
                            assert cookie.contains("HttpOnly") : "accessToken 쿠키는 HttpOnly여야 합니다";
                            foundAccessTokenWithHttpOnly = true;
                        }
                    }

                    assert foundAccessTokenWithHttpOnly : "accessToken 쿠키를 찾을 수 없습니다";
                });
    }

    @Test
    @DisplayName("Refresh Token 쿠키는 HttpOnly=true (XSS 방어)")
    void refreshTokenCookie_IsHttpOnly() throws Exception {
        // given
        String refreshToken = jwtProvider.generateRefreshToken();
        refreshTokenRedisRepository.save(refreshToken, testUser.getId(), 7 * 24 * 60 * 60 * 1000L);

        // when & then
        mockMvc.perform(post("/auth/refresh")
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    var setCookieHeaders = result.getResponse().getHeaders("Set-Cookie");
                    boolean foundRefreshTokenWithHttpOnly = false;

                    // refreshToken 쿠키 찾기
                    for (String cookie : setCookieHeaders) {
                        if (cookie.contains("refreshToken=")) {
                            // HttpOnly가 있어야 함
                            assert cookie.contains("HttpOnly") : "refreshToken 쿠키는 HttpOnly여야 합니다";
                            foundRefreshTokenWithHttpOnly = true;
                        }
                    }

                    assert foundRefreshTokenWithHttpOnly : "refreshToken 쿠키를 찾을 수 없습니다";
                });
    }
}
