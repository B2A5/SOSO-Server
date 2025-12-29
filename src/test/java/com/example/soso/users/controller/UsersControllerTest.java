package com.example.soso.users.controller;

import com.example.soso.security.domain.CustomUserDetails;
import com.example.soso.users.domain.dto.UserResponse;
import com.example.soso.users.domain.entity.*;
import com.example.soso.users.service.UsersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
        "jwt.secret-key=test-jwt-secret-key-that-is-sufficiently-long-and-secure-for-testing-purposes-minimum-256-bits-required-by-jwt-library",
        "jwt.access-token-validity-in-ms=1800000",
        "jwt.refresh-token-validity-in-ms=1209600000"
})
@Transactional
@DisplayName("사용자 정보 컨트롤러 테스트")
class UsersControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private UsersService usersService;

    private MockMvc mockMvc;
    private CustomUserDetails testUserDetails;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        Users testUser = Users.builder()
                .username("홍길동")
                .nickname("길동이")
                .email("test@example.com")
                .userType(UserType.FOUNDER)
                .profileImageUrl("https://example.com/profile.jpg")
                .gender(Gender.MALE)
                .ageRange(AgeRange.TWENTIES)
                .budget(BudgetRange.THOUSANDS_3000_5000)
                .startupExperience(StartupExperience.YES)
                .location("서울시 강남구")
                .interests(Arrays.asList(InterestType.MANUFACTURING, InterestType.WHOLESALE_RETAIL))
                .latitude("37.5665")
                .longitude("126.9780")
                .build();

        try {
            java.lang.reflect.Field idField = Users.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testUser, "test-user-id-123");
        } catch (Exception e) {
            throw new RuntimeException("Failed to set user id", e);
        }

        testUserDetails = new CustomUserDetails(testUser);
    }

    @Test
    @DisplayName("본인 정보 조회 - 성공")
    void getCurrentUser_Success() throws Exception {
        // Given
        UserResponse mockResponse = UserResponse.builder()
                .id("test-user-id-123")
                .username("홍길동")
                .nickname("길동이")
                .email("test@example.com")
                .userType(UserType.FOUNDER)
                .profileImageUrl("https://example.com/profile.jpg")
                .gender(Gender.MALE)
                .ageRange(AgeRange.TWENTIES)
                .budget("3~5천")
                .startupExperience("창업 경험 유")
                .location("서울시 강남구")
                .interests(Arrays.asList("식료품 등 제조업", "도매 및 소매업"))
                .latitude("37.5665")
                .longitude("126.9780")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(usersService.getCurrentUserInfo(anyString())).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/users/me")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("test-user-id-123"))
                .andExpect(jsonPath("$.username").value("홍길동"))
                .andExpect(jsonPath("$.nickname").value("길동이"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.userType").value("FOUNDER"))
                .andExpect(jsonPath("$.gender").value("MALE"))
                .andExpect(jsonPath("$.ageRange").value("TWENTIES"))
                .andExpect(jsonPath("$.budget").value("3~5천"))
                .andExpect(jsonPath("$.startupExperience").value("창업 경험 유"))
                .andExpect(jsonPath("$.location").value("서울시 강남구"))
                .andExpect(jsonPath("$.interests[0]").value("식료품 등 제조업"))
                .andExpect(jsonPath("$.interests[1]").value("도매 및 소매업"))
                .andExpect(jsonPath("$.latitude").value("37.5665"))
                .andExpect(jsonPath("$.longitude").value("126.9780"));
    }

}
