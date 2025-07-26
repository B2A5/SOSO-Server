package com.example.soso.users.controller;

import com.example.soso.global.jwt.JwtTokenDto;
import com.example.soso.users.domain.dto.*;
import com.example.soso.users.domain.entity.InterestRequest;
import com.example.soso.users.domain.entity.SignupStep;
import com.example.soso.users.service.SignupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Signup - Founder", description = """
예비 창업자 회원가입 플로우

1. /signup/founder/user-type  
2. /signup/founder/region  
3. /signup/founder/age-range  
4. /signup/founder/gender  
5. /signup/founder/interests  
6. /signup/founder/budget  
7. /signup/founder/experience  
8. /signup/founder/nickname  
9. /signup/founder/complete
""")
@RestController
@RequestMapping("/signup/founder")
@RequiredArgsConstructor
public class FounderSignupController {

    private final SignupService signupService;

    @Operation(summary = "[1단계] 유저 타입 설정")
    @PostMapping("/user-type")
    public ResponseEntity<SignupStep> setUserType(@RequestBody @Valid UserTypeRequest request,
                                                  HttpSession session) {
        SignupStep nextStep = signupService.saveUserType(session, request.userType());
        return ResponseEntity.ok(nextStep);
    }

    @Operation(summary = "[2단계] 지역 설정")
    @PostMapping("/region")
    public ResponseEntity<SignupStep> setRegion(@RequestBody @Valid RegionRequest request,
                                                HttpSession session) {
        SignupStep nextStep = signupService.saveRegion(session, request.regionId());
        return ResponseEntity.ok(nextStep);
    }

    @Operation(summary = "[3단계] 나이대 설정")
    @PostMapping("/age-range")
    public ResponseEntity<SignupStep> setAgeRange(@RequestBody @Valid AgeRangeRequest request,
                                                  HttpSession session) {
        SignupStep nextStep = signupService.saveAgeRange(session, request.ageRange());
        return ResponseEntity.ok(nextStep);
    }

    @Operation(summary = "[4단계] 성별 설정")
    @PostMapping("/gender")
    public ResponseEntity<SignupStep> setGender(@RequestBody @Valid GenderRequest request,
                                                HttpSession session) {
        SignupStep nextStep = signupService.saveGender(session, request.gender());
        return ResponseEntity.ok(nextStep);
    }

    @Operation(summary = "[5단계] 관심사 설정")
    @PostMapping("/interests")
    public ResponseEntity<SignupStep> setInterests(@RequestBody @Valid InterestRequest request,
                                                   HttpSession session) {
        SignupStep nextStep = signupService.saveInterests(session, request.interests());
        return ResponseEntity.ok(nextStep);
    }

    @Operation(summary = "[6단계] 예산 설정")
    @PostMapping("/budget")
    public ResponseEntity<SignupStep> setBudget(@RequestBody BudgetRequest request,
                                                HttpSession session) {
        SignupStep nextStep = signupService.saveBudget(session, request.budget());
        return ResponseEntity.ok(nextStep);
    }

    @Operation(summary = "[7단계] 창업 경험 설정")
    @PostMapping("/experience")
    public ResponseEntity<SignupStep> setExperience(@RequestBody @Valid ExperienceRequest request,
                                                    HttpSession session) {
        SignupStep nextStep = signupService.saveExperience(session, request.experience());
        return ResponseEntity.ok(nextStep);
    }

    @Operation(summary = "[8단계] 닉네임 생성")
    @PostMapping("/nickname")
    public ResponseEntity<String> saveNickname(HttpSession session) {
        String nickname = signupService.saveNiceName(session);
        return ResponseEntity.ok(nickname);
    }

    @Operation(summary = "[9단계] 회원가입 완료")
    @PostMapping("/complete")
    public ResponseEntity<JwtTokenDto> completeSignup(HttpSession session,
                                                      HttpServletResponse response) {
        JwtTokenDto jwtAccessToken = signupService.completeSignup(session, response);
        return ResponseEntity.ok(jwtAccessToken);
    }
}
