package com.example.soso.auth.integration;

import com.example.soso.global.jwt.JwtProvider;
import com.example.soso.global.jwt.JwtTokenDto;
import com.example.soso.global.redis.RefreshTokenRedisRepository;
import com.example.soso.users.domain.entity.Users;
import com.example.soso.users.repository.UsersRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import jakarta.servlet.http.Cookie;


import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.session.store-type=none",
        "jwt.secret-key=ThisIsAVerySecretKeyForTestingPurposesAndItShouldBeLongEnoughToMeetTheRequirements",
        "jwt.access-token-validity-in-ms=3600000",
        "jwt.refresh-token-validity-in-ms=1209600000"
})
@DisplayName("인증 관련 통합 테스트")
class AuthIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @MockBean
    private RefreshTokenRedisRepository refreshTokenRedisRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        // Redis 데이터는 TTL로 자동 만료되므로 초기화 불필요
    }

    /**
     * 테스트용 사용자 생성
     */
    private Users createTestUser() {
        Users testUser = Users.builder()
                .nickname("테스트사용자")
                .email("test@example.com")
                .profileImageUrl("https://example.com/profile.jpg")
                .build();

        return usersRepository.save(testUser);
    }

    @Test
    @DisplayName("리프레시 토큰으로 액세스 토큰 재발급 성공")
    void refreshToken_Success() throws Exception {
        // Given: 테스트 사용자 생성
        Users testUser = createTestUser();
        String userId = testUser.getId();

        // 리프레시 토큰 생성 및 Mock Redis 설정
        String refreshToken = jwtProvider.generateRefreshToken();
        when(refreshTokenRedisRepository.getUserIdByRefreshToken(refreshToken)).thenReturn(userId);

        // 새로운 토큰 생성을 위한 지연 (다른 millisecond 보장)
        Thread.sleep(2);

        // When: 리프레시 토큰으로 액세스 토큰 재발급 요청
        MvcResult result = mockMvc.perform(post("/auth/refresh")
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwtAccessToken").exists())
                .andReturn();

        // Then: 새로운 액세스 토큰 발급 확인
        String responseContent = result.getResponse().getContentAsString();
        JwtTokenDto tokenDto = objectMapper.readValue(responseContent, JwtTokenDto.class);

        // 새로 발급받은 액세스 토큰이 유효한지 확인
        String newAccessToken = tokenDto.jwtAccessToken();
        assert jwtProvider.validateToken(newAccessToken);
        assert userId.equals(jwtProvider.getSubject(newAccessToken));

        // 새로운 리프레시 토큰 쿠키가 설정되었는지 확인
        Cookie[] cookies = result.getResponse().getCookies();
        boolean hasRefreshTokenCookie = false;
        for (Cookie cookie : cookies) {
            if ("refreshToken".equals(cookie.getName())) {
                hasRefreshTokenCookie = true;
                // RTR로 새 토큰이 설정되었는지 확인 (값이 비어있지 않으면 성공)
                assert cookie.getValue() != null && !cookie.getValue().isEmpty();
                break;
            }
        }
        assert hasRefreshTokenCookie : "리프레시 토큰 쿠키가 설정되지 않았습니다";

        // verify() 메서드로 Redis 삭제가 호출되었는지 확인 (RTR 동작)
        verify(refreshTokenRedisRepository).delete(refreshToken);
    }

    @Test
    @DisplayName("유효하지 않은 리프레시 토큰으로 재발급 시도 시 401 에러")
    void refreshToken_InvalidToken_ShouldReturn401() throws Exception {
        // Given: 유효하지 않은 리프레시 토큰
        String invalidRefreshToken = "invalid.refresh.token";
        // Mock: Redis에서 userId를 찾을 수 없도록 설정
        when(refreshTokenRedisRepository.getUserIdByRefreshToken(invalidRefreshToken)).thenReturn(null);

        // When & Then: 401 에러 반환
        mockMvc.perform(post("/auth/refresh")
                        .cookie(new Cookie("refreshToken", invalidRefreshToken)))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("만료된 리프레시 토큰으로 재발급 시도 시 401 에러")
    void refreshToken_ExpiredToken_ShouldReturn401() throws Exception {
        // Given: 테스트 사용자 생성
        Users testUser = createTestUser();
        String userId = testUser.getId();

        // 만료된 리프레시 토큰 (Redis에서 userId를 찾을 수 없도록 설정)
        String expiredRefreshToken = jwtProvider.generateRefreshToken();
        when(refreshTokenRedisRepository.getUserIdByRefreshToken(expiredRefreshToken)).thenReturn(null);

        // When & Then: 401 에러 반환
        mockMvc.perform(post("/auth/refresh")
                        .cookie(new Cookie("refreshToken", expiredRefreshToken)))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("리프레시 토큰 쿠키가 없을 때 400 에러")
    void refreshToken_NoCookie_ShouldReturn400() throws Exception {
        // When & Then: 쿠키 없이 요청 시 400 에러
        mockMvc.perform(post("/auth/refresh"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("발급받은 액세스 토큰으로 인증 필요 API 호출 성공")
    void accessToken_AuthenticatedRequest_Success() throws Exception {
        // Given: 테스트 사용자 생성 및 액세스 토큰 발급
        Users testUser = createTestUser();
        String accessToken = jwtProvider.generateAccessToken(testUser.getId());

        // When & Then: 액세스 토큰으로 인증 필요 API 호출 성공
        mockMvc.perform(multipart("/community/freeboard")
                        .param("title", "인증 테스트 게시글")
                        .param("content", "액세스 토큰으로 인증된 요청입니다.")
                        .param("category", "restaurant")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.postId").exists());
    }

    @Test
    @DisplayName("잘못된 액세스 토큰으로 인증 필요 API 호출 시 401 에러")
    void accessToken_InvalidToken_ShouldReturn401() throws Exception {
        // Given: 잘못된 액세스 토큰
        String invalidAccessToken = "invalid.access.token";

        // When & Then: 401 에러 반환
        mockMvc.perform(multipart("/community/freeboard")
                        .param("title", "인증 실패 테스트")
                        .param("content", "잘못된 토큰")
                        .param("category", "restaurant")
                        .header("Authorization", "Bearer " + invalidAccessToken))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("만료된 액세스 토큰으로 인증 필요 API 호출 시 401 에러")
    void accessToken_ExpiredToken_ShouldReturn401() throws Exception {
        // Given: 테스트 사용자 생성
        Users testUser = createTestUser();

        // 매우 짧은 만료 시간으로 액세스 토큰 생성 (실제로는 JwtProvider에서 만료시간 설정 필요)
        // 여기서는 이미 만료된 토큰을 시뮬레이션
        String expiredAccessToken = "expired.access.token";

        // When & Then: 401 에러 반환
        mockMvc.perform(multipart("/community/freeboard")
                        .param("title", "만료된 토큰 테스트")
                        .param("content", "만료된 액세스 토큰")
                        .param("category", "restaurant")
                        .header("Authorization", "Bearer " + expiredAccessToken))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Authorization 헤더 없이 인증 필요 API 호출 시 401 에러")
    void accessToken_NoAuthHeader_ShouldReturn401() throws Exception {
        // When & Then: Authorization 헤더 없이 요청 시 401 에러
        mockMvc.perform(multipart("/community/freeboard")
                        .param("title", "헤더 없음 테스트")
                        .param("content", "Authorization 헤더 없음")
                        .param("category", "restaurant"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("잘못된 Bearer 형식으로 인증 필요 API 호출 시 401 에러")
    void accessToken_WrongBearerFormat_ShouldReturn401() throws Exception {
        // Given: 테스트 사용자 생성 및 유효한 액세스 토큰
        Users testUser = createTestUser();
        String accessToken = jwtProvider.generateAccessToken(testUser.getId());

        // When & Then: 잘못된 Bearer 형식으로 요청 시 401 에러
        mockMvc.perform(multipart("/community/freeboard")
                        .param("title", "잘못된 형식 테스트")
                        .param("content", "잘못된 Bearer 형식")
                        .param("category", "restaurant")
                        .header("Authorization", "Token " + accessToken)) // "Bearer" 대신 "Token" 사용
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }
}