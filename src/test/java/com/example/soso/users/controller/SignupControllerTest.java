package com.example.soso.users.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.soso.users.domain.dto.ExperienceRequest;
import com.example.soso.users.domain.dto.SignupCompleteResponse;
import com.example.soso.users.domain.dto.UserResponse;
import com.example.soso.users.domain.entity.AgeRange;
import com.example.soso.users.domain.entity.Gender;
import com.example.soso.users.domain.entity.SignupStep;
import com.example.soso.users.domain.entity.StartupExperience;
import com.example.soso.users.domain.entity.UserType;
import com.example.soso.users.service.SignupService;
import java.time.LocalDateTime;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class SignupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SignupService signupService;


    @Test
    @DisplayName("경험 단계가 정상 처리되면 다음 단계 정보를 반환한다")
    void postExperience() throws Exception {
        when(signupService.saveExperience(any(HttpSession.class), eq(StartupExperience.YES)))
                .thenReturn(SignupStep.NICKNAME);

        mockMvc.perform(post("/signup/experience")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ExperienceRequest(StartupExperience.YES))))
                .andExpect(status().isOk())
                .andExpect(content().json("\"NICKNAME\""));
    }

    @Test
    @DisplayName("닉네임 생성이 성공하면 생성된 닉네임을 반환한다")
    void postNickname() throws Exception {
        when(signupService.saveNickname(any(HttpSession.class))).thenReturn("닉네임");

        mockMvc.perform(post("/signup/nickname"))
                .andExpect(status().isOk())
                .andExpect(content().string("닉네임"));
    }

    @Test
    @DisplayName("회원가입 완료 시 JWT 토큰 및 사용자 정보를 반환한다")
    void postComplete() throws Exception {
        UserResponse userResponse = UserResponse.builder()
                .id("test-id")
                .username("테스트")
                .nickname("테스터")
                .email("test@example.com")
                .userType(UserType.FOUNDER)
                .gender(Gender.MALE)
                .ageRange(AgeRange.TWENTIES)
                .location("서울시 강남구")
                .createdDate(LocalDateTime.now())
                .lastModifiedDate(LocalDateTime.now())
                .build();

        SignupCompleteResponse response = new SignupCompleteResponse("access", userResponse);

        when(signupService.completeSignup(any(HttpSession.class), any(HttpServletResponse.class)))
                .thenReturn(response);

        mockMvc.perform(post("/signup/complete"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"accessToken\":\"access\"}"));
    }

    @Test
    @DisplayName("창업 경험 데이터 조회")
    void getExperienceData() throws Exception {
        when(signupService.getExperience(any(HttpSession.class)))
                .thenReturn(new ExperienceRequest(StartupExperience.NO));

        mockMvc.perform(get("/signup/experience/data"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"experience\":\"NO\"}"));
    }
}
