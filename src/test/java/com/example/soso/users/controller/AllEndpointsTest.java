package com.example.soso.users.controller;

import com.example.soso.users.domain.entity.AgeRange;
import com.example.soso.users.domain.entity.BudgetRange;
import com.example.soso.users.domain.entity.Gender;
import com.example.soso.users.domain.entity.InterestType;
import com.example.soso.users.domain.entity.SignupStep;
import com.example.soso.users.domain.entity.UserType;
import com.example.soso.users.service.SignupService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@DisplayName("전체 회원가입 엔드포인트 단위 테스트")
class AllEndpointsTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private SignupService signupService;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private MockHttpSession mockSession;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        mockSession = new MockHttpSession();
    }

    @Test
    @DisplayName("[공통] 모든 단계별 엔드포인트 테스트")
    void testAllCommonEndpoints() throws Exception {
        testUserTypeEndpoint();
        testRegionEndpoint();
        testAgeRangeEndpoint();
        // testGenderEndpoint(); // 별도 테스트로 분리
        testInterestsEndpoint();
    }

    @Test
    @DisplayName("[1단계] 유저타입 엔드포인트 - INHABITANT")
    void testUserTypeInhabitant() throws Exception {
        when(signupService.saveUserType(any(HttpSession.class), eq(UserType.INHABITANT)))
                .thenReturn(SignupStep.REGION);

        mockMvc.perform(post("/signup/user-type")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "userType": "INHABITANT"
                                }
                                """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("REGION"));
    }

    @Test
    @DisplayName("[1단계] 유저타입 엔드포인트 - FOUNDER")
    void testUserTypeFounder() throws Exception {
        when(signupService.saveUserType(any(HttpSession.class), eq(UserType.FOUNDER)))
                .thenReturn(SignupStep.REGION);

        mockMvc.perform(post("/signup/user-type")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "userType": "FOUNDER"
                                }
                                """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("REGION"));
    }

    @Test
    @DisplayName("[2단계] 지역 엔드포인트")
    void testRegionEndpoint() throws Exception {
        when(signupService.saveRegion(any(HttpSession.class), eq("SEOUL")))
                .thenReturn(SignupStep.AGE);

        mockMvc.perform(post("/signup/region")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "regionId": "SEOUL"
                                }
                                """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("AGE"));
    }

    @Test
    @DisplayName("[3단계] 연령대 엔드포인트 - 모든 연령대")
    void testAgeRangeEndpoint() throws Exception {
        // TWENTIES
        when(signupService.saveAgeRange(any(HttpSession.class), eq(AgeRange.TWENTIES)))
                .thenReturn(SignupStep.GENDER);

        mockMvc.perform(post("/signup/age-range")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "ageRange": "TWENTIES"
                                }
                                """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("GENDER"));

        // THIRTIES
        when(signupService.saveAgeRange(any(HttpSession.class), eq(AgeRange.THIRTIES)))
                .thenReturn(SignupStep.GENDER);

        mockMvc.perform(post("/signup/age-range")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "ageRange": "THIRTIES"
                                }
                                """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("GENDER"));
    }

    @Test
    @DisplayName("[4단계] 성별 엔드포인트 - INHABITANT")
    void testGenderEndpointInhabitant() throws Exception {
        when(signupService.saveGender(any(HttpSession.class), eq(Gender.MALE)))
                .thenReturn(SignupStep.NICKNAME);

        mockMvc.perform(post("/signup/gender")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "gender": "MALE"
                                }
                                """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("NICKNAME"));
    }

    @Test
    @DisplayName("[4단계] 성별 엔드포인트 - FOUNDER")
    void testGenderEndpointFounder() throws Exception {
        when(signupService.saveGender(any(HttpSession.class), eq(Gender.FEMALE)))
                .thenReturn(SignupStep.INTERESTS);

        mockMvc.perform(post("/signup/gender")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "gender": "FEMALE"
                                }
                                """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("INTERESTS"));
    }

    @Test
    @DisplayName("[6단계] 예산 엔드포인트 - 영어 enum 이름 테스트")
    void testBudgetEndpoint() throws Exception {
        when(signupService.saveBudget(any(HttpSession.class), any(BudgetRange.class)))
                .thenReturn(SignupStep.STARTUP);

        // 영어 enum 이름으로 테스트
        mockMvc.perform(post("/signup/budget")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "budget": "UNDER_1000"
                                }
                                """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("STARTUP"));

        // 다른 예산 구간 테스트
        mockMvc.perform(post("/signup/budget")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "budget": "THOUSANDS_2000"
                                }
                                """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("STARTUP"));

        // 한국어 라벨로 테스트
        mockMvc.perform(post("/signup/budget")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "budget": "1천 이하"
                                }
                                """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("STARTUP"));

        // 다른 예산 구간 테스트
        mockMvc.perform(post("/signup/budget")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "budget": "OVER_1B"
                                }
                                """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("STARTUP"));
    }

    @Test
    @DisplayName("[5단계] 관심업종 엔드포인트 - 영어 enum 이름 테스트")
    void testInterestsEndpoint() throws Exception {
        when(signupService.saveInterests(any(HttpSession.class), any(List.class)))
                .thenReturn(SignupStep.BUDGET);

        // 영어 enum 이름으로 테스트
        mockMvc.perform(post("/signup/interests")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "interests": ["MANUFACTURING", "WHOLESALE_RETAIL"]
                                }
                                """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("BUDGET"));

        // 단일 관심업종 테스트
        mockMvc.perform(post("/signup/interests")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "interests": ["OTHER"]
                                }
                                """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("BUDGET"));

        // 모든 관심업종 테스트
        mockMvc.perform(post("/signup/interests")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "interests": ["MANUFACTURING", "WHOLESALE_RETAIL", "TRANSPORT", "ACCOMMODATION_FOOD", "WELFARE", "ART_SPORTS", "OTHER"]
                                }
                                """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("BUDGET"));
    }

    @Test
    @DisplayName("잘못된 데이터 입력 테스트")
    void testInvalidInputs() throws Exception {
        // 잘못된 유저타입
        mockMvc.perform(post("/signup/user-type")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "userType": "INVALID_TYPE"
                                }
                                """))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // 잘못된 연령대
        mockMvc.perform(post("/signup/age-range")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "ageRange": "INVALID_AGE"
                                }
                                """))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // 잘못된 관심업종
        mockMvc.perform(post("/signup/interests")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "interests": ["INVALID_INTEREST"]
                                }
                                """))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // 필수 필드 누락
        mockMvc.perform(post("/signup/user-type")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("InterestType enum JSON 직렬화/역직렬화 테스트")
    void testInterestTypeEnumSerialization() throws Exception {
        // InterestType enum이 영어 이름과 한국어 라벨 모두 처리할 수 있는지 테스트

        // 영어 enum 이름으로 역직렬화 테스트
        InterestType manufacturing = InterestType.fromValue("MANUFACTURING");
        assert manufacturing == InterestType.MANUFACTURING;

        InterestType wholesale = InterestType.fromValue("WHOLESALE_RETAIL");
        assert wholesale == InterestType.WHOLESALE_RETAIL;

        // 한국어 라벨로 역직렬화 테스트
        InterestType manufacturingKor = InterestType.fromValue("식료품 등 제조업");
        assert manufacturingKor == InterestType.MANUFACTURING;

        InterestType wholesaleKor = InterestType.fromValue("도매 및 소매업");
        assert wholesaleKor == InterestType.WHOLESALE_RETAIL;

        // JSON 직렬화 테스트 (한국어 라벨로 출력)
        assert InterestType.MANUFACTURING.getLabel().equals("식료품 등 제조업");
        assert InterestType.WHOLESALE_RETAIL.getLabel().equals("도매 및 소매업");
    }

    private void testUserTypeEndpoint() throws Exception {
        when(signupService.saveUserType(any(HttpSession.class), eq(UserType.FOUNDER)))
                .thenReturn(SignupStep.REGION);

        mockMvc.perform(post("/signup/user-type")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "userType": "FOUNDER"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("REGION"));
    }
}