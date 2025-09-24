package com.example.soso.users.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.soso.global.jwt.JwtTokenDto;
import com.example.soso.users.domain.dto.ExperienceRequest;
import com.example.soso.users.domain.entity.SignupStep;
import com.example.soso.users.domain.entity.StartupExperience;
import com.example.soso.users.service.SignupService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@WebMvcTest(value = SignupController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
}, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        com.example.soso.security.config.SecurityConfig.class,
        com.example.soso.security.filter.JwtAuthenticationFilter.class,
        com.example.soso.security.filter.ExceptionHandlerFilter.class
}))
@AutoConfigureMockMvc(addFilters = false)
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
    @DisplayName("회원가입 완료 시 JWT 토큰을 반환한다")
    void postComplete() throws Exception {
        when(signupService.completeSignup(any(HttpSession.class), any(HttpServletResponse.class)))
                .thenReturn(new JwtTokenDto("access"));

        mockMvc.perform(post("/signup/complete"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"jwtAccessToken\":\"access\"}"));
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
