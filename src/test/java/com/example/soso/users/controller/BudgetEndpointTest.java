package com.example.soso.users.controller;

import com.example.soso.users.domain.entity.BudgetRange;
import com.example.soso.users.domain.entity.SignupStep;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@DisplayName("예산 선택 엔드포인트 테스트")
class BudgetEndpointTest {

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
    @DisplayName("예산 엔드포인트 - UNDER_1000")
    void testBudgetEndpoint_Under1000() throws Exception {
        when(signupService.saveBudget(any(HttpSession.class), eq(BudgetRange.UNDER_1000)))
                .thenReturn(SignupStep.STARTUP);

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
    }

    @Test
    @DisplayName("예산 엔드포인트 - THOUSANDS_2000")
    void testBudgetEndpoint_Thousands2000() throws Exception {
        when(signupService.saveBudget(any(HttpSession.class), eq(BudgetRange.THOUSANDS_2000)))
                .thenReturn(SignupStep.STARTUP);

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
    }

    @Test
    @DisplayName("예산 엔드포인트 - 모든 예산 구간 테스트")
    void testAllBudgetRanges() throws Exception {
        // 모든 BudgetRange enum 값들을 테스트
        BudgetRange[] budgetRanges = {
                BudgetRange.UNDER_1000,
                BudgetRange.THOUSANDS_2000,
                BudgetRange.THOUSANDS_3000_5000,
                BudgetRange.THOUSANDS_5000_7000,
                BudgetRange.THOUSANDS_7000_TO_1B,
                BudgetRange.OVER_1B
        };

        for (BudgetRange budgetRange : budgetRanges) {
            when(signupService.saveBudget(any(HttpSession.class), eq(budgetRange)))
                    .thenReturn(SignupStep.STARTUP);

            mockMvc.perform(post("/signup/budget")
                            .session(mockSession)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                        "budget": "%s"
                                    }
                                    """, budgetRange.name())))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value("STARTUP"));
        }
    }

    @Test
    @DisplayName("BudgetRange enum JSON 직렬화/역직렬화 테스트")
    void testBudgetRangeEnumSerialization() throws Exception {
        // 영어 enum 이름으로 역직렬화 테스트
        BudgetRange under1000 = BudgetRange.fromValue("UNDER_1000");
        assert under1000 == BudgetRange.UNDER_1000;

        BudgetRange thousands2000 = BudgetRange.fromValue("THOUSANDS_2000");
        assert thousands2000 == BudgetRange.THOUSANDS_2000;

        BudgetRange over1B = BudgetRange.fromValue("OVER_1B");
        assert over1B == BudgetRange.OVER_1B;

        // 한국어 라벨로 역직렬화 테스트
        BudgetRange under1000Kor = BudgetRange.fromValue("1천 이하");
        assert under1000Kor == BudgetRange.UNDER_1000;

        BudgetRange thousands2000Kor = BudgetRange.fromValue("2천대");
        assert thousands2000Kor == BudgetRange.THOUSANDS_2000;

        BudgetRange over1BKor = BudgetRange.fromValue("1억 이상");
        assert over1BKor == BudgetRange.OVER_1B;

        // JSON 직렬화 테스트 (한국어 라벨로 출력)
        assert BudgetRange.UNDER_1000.getLabel().equals("1천 이하");
        assert BudgetRange.THOUSANDS_2000.getLabel().equals("2천대");
        assert BudgetRange.THOUSANDS_3000_5000.getLabel().equals("3~5천");
        assert BudgetRange.OVER_1B.getLabel().equals("1억 이상");
    }

    @Test
    @DisplayName("잘못된 예산 데이터 입력 테스트")
    void testInvalidBudgetInput() throws Exception {
        // 잘못된 예산 타입
        mockMvc.perform(post("/signup/budget")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "budget": "INVALID_BUDGET"
                                }
                                """))
                .andDo(print())
                .andExpect(status().isInternalServerError());

        // null budget
        mockMvc.perform(post("/signup/budget")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "budget": null
                                }
                                """))
                .andDo(print())
                .andExpect(status().isOk()); // budget은 optional이므로 null 허용

        // 필드 누락 (budget 필드 자체가 없음)
        mockMvc.perform(post("/signup/budget")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andDo(print())
                .andExpect(status().isOk()); // budget은 optional이므로 누락 허용
    }

    @Test
    @DisplayName("한국어 라벨로 예산 설정 테스트")
    void testBudgetWithKoreanLabels() throws Exception {
        when(signupService.saveBudget(any(HttpSession.class), eq(BudgetRange.UNDER_1000)))
                .thenReturn(SignupStep.STARTUP);

        // 한국어 라벨로 요청
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
    }

    @Test
    @DisplayName("예산 건너뛰기 테스트 (null budget)")
    void testSkipBudget() throws Exception {
        when(signupService.saveBudget(any(HttpSession.class), eq(null)))
                .thenReturn(SignupStep.STARTUP);

        // budget이 null인 경우 (건너뛰기)
        mockMvc.perform(post("/signup/budget")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "budget": null
                                }
                                """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("STARTUP"));

        // budget 필드가 아예 없는 경우
        when(signupService.saveBudget(any(HttpSession.class), eq(null)))
                .thenReturn(SignupStep.STARTUP);

        mockMvc.perform(post("/signup/budget")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("STARTUP"));
    }
}