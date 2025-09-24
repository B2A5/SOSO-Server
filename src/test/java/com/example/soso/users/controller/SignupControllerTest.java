package com.example.soso.users.controller;

import com.example.soso.users.domain.entity.AgeRange;
import com.example.soso.users.domain.entity.Gender;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@DisplayName("공통 회원가입 컨트롤러 테스트")
class SignupControllerTest {

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
    @DisplayName("유저 타입 설정 성공 테스트")
    void setUserType_Success() throws Exception {
        // given
        when(signupService.saveUserType(any(HttpSession.class), eq(UserType.INHABITANT)))
                .thenReturn(SignupStep.REGION);

        String requestBody = """
                {
                    "userType": "INHABITANT"
                }
                """;

        // when & then
        mockMvc.perform(post("/signup/user-type")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("REGION"));
    }

    @Test
    @DisplayName("지역 설정 성공 테스트")
    void setRegion_Success() throws Exception {
        // given
        when(signupService.saveRegion(any(HttpSession.class), eq("SEOUL")))
                .thenReturn(SignupStep.AGE);

        String requestBody = """
                {
                    "regionId": "SEOUL"
                }
                """;

        // when & then
        mockMvc.perform(post("/signup/region")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("AGE"));
    }

    @Test
    @DisplayName("연령대 설정 성공 테스트")
    void setAgeRange_Success() throws Exception {
        // given
        when(signupService.saveAgeRange(any(HttpSession.class), eq(AgeRange.TWENTIES)))
                .thenReturn(SignupStep.GENDER);

        String requestBody = """
                {
                    "ageRange": "TWENTIES"
                }
                """;

        // when & then
        mockMvc.perform(post("/signup/age-range")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("GENDER"));
    }

    @Test
    @DisplayName("성별 설정 성공 테스트")
    void setGender_Success() throws Exception {
        // given
        when(signupService.saveGender(any(HttpSession.class), eq(Gender.MALE)))
                .thenReturn(SignupStep.NINAME);

        String requestBody = """
                {
                    "gender": "MALE"
                }
                """;

        // when & then
        mockMvc.perform(post("/signup/gender")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("NINAME"));
    }

    @Test
    @DisplayName("관심업종 설정 성공 테스트")
    void setInterests_Success() throws Exception {
        // given
        when(signupService.saveInterests(any(HttpSession.class), any(List.class)))
                .thenReturn(SignupStep.BUDGET);

        String requestBody = """
                {
                    "interests": ["MANUFACTURING", "WHOLESALE_RETAIL"]
                }
                """;

        // when & then
        mockMvc.perform(post("/signup/interests")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("BUDGET"));
    }

    @Test
    @DisplayName("유효하지 않은 유저 타입으로 요청시 실패")
    void setUserType_InvalidUserType() throws Exception {
        String requestBody = """
                {
                    "userType": "INVALID_TYPE"
                }
                """;

        mockMvc.perform(post("/signup/user-type")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("유효하지 않은 연령대로 요청시 실패")
    void setAgeRange_InvalidAgeRange() throws Exception {
        String requestBody = """
                {
                    "ageRange": "INVALID_AGE"
                }
                """;

        mockMvc.perform(post("/signup/age-range")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("필수 값 누락시 실패")
    void setUserType_MissingRequiredField() throws Exception {
        String requestBody = """
                {
                }
                """;

        mockMvc.perform(post("/signup/user-type")
                        .session(mockSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }
}