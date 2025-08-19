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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 프론트 레거시 경로(/signup/...)를 유지하면서
 * 내부 SignupService/SignupFlow의 단계 검증 로직을 그대로 사용하는 통합 컨트롤러.
 *
 * Founder/ Inhabitant 분기는 컨트롤러가 아니라 서비스의 단계(FSM) 검증으로 처리됩니다.
 */
@Tag(name = "Signup - Unified", description = """
프론트 레거시 경로(/signup/...) 호환 단일 컨트롤러

1. /signup/user-type
2. /signup/region
3. /signup/age-range
4. /signup/gender
5. /signup/interests        (Founder 전용)
6. /signup/budget           (Founder 전용)
7. /signup/experience       (Founder 전용)
8. /signup/nickname
9. /signup/complete

(뒤로가기 조회)
- /signup/region/data
- /signup/age-range/data
- /signup/gender/data
- /signup/interests/data
- /signup/budget/data
- /signup/experience/data
""")
@RestController
@RequestMapping("/signup")
@RequiredArgsConstructor
public class SignupController {

    private final SignupService signupService;

    // ---------------------------
    // 진행 스텝 (POST)
    // ---------------------------

    @Operation(summary = "[1단계] 유저 타입 설정")
    @PostMapping("/user-type")
    public ResponseEntity<SignupStep> setUserType(@RequestBody @Valid UserTypeRequest request,
                                                  HttpSession session) {
        // 세션이 없다면 여기에서 생성하는 가드 로직을 추가해도 됩니다.
        // initSignupIfAbsent(session);  // 필요 시 구현
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

    @Operation(summary = "[5단계] 관심사 설정 (Founder 전용)")
    @PostMapping("/interests")
    public ResponseEntity<SignupStep> setInterests(@RequestBody @Valid InterestRequest request,
                                                   HttpSession session) {
        // null 값들을 필터링 (프론트에서 빈 문자열을 보낼 경우 @JsonCreator에서 null로 변환됨)
        var filteredInterests = request.interests() != null ? 
            request.interests().stream()
                .filter(java.util.Objects::nonNull)
                .toList() : 
            null;
        
        SignupStep nextStep = signupService.saveInterests(session, filteredInterests);
        return ResponseEntity.ok(nextStep);
    }

    @Operation(summary = "[6단계] 예산 설정 (Founder 전용)")
    @PostMapping("/budget")
    public ResponseEntity<SignupStep> setBudget(@RequestBody BudgetRequest request,
                                                HttpSession session) {
        SignupStep nextStep = signupService.saveBudget(session, request.budget());
        return ResponseEntity.ok(nextStep);
    }

    @Operation(summary = "[7단계] 창업 경험 설정 (Founder 전용)")
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

    @Operation(summary = "[9단계] 회원가입 완료 (프론트 호환 응답)")
    @PostMapping("/complete")
    public ResponseEntity<SignupCompleteResponse> completeSignup(HttpSession session,
                                                                 HttpServletResponse response) {
        JwtTokenDto tokenDto = signupService.completeSignup(session, response);
        return ResponseEntity.ok(new SignupCompleteResponse(tokenDto.jwtAccessToken()));
    }

    // ---------------------------
    // 뒤로가기 조회 (GET)
    // ---------------------------

    @Operation(summary = "[뒤로가기] 지역 정보 조회")
    @GetMapping("/region/data")
    public ResponseEntity<RegionRequest> getRegion(HttpSession session) {
        return ResponseEntity.ok(signupService.getRegion(session));
    }

    @Operation(summary = "[뒤로가기] 나이대 정보 조회")
    @GetMapping("/age-range/data")
    public ResponseEntity<AgeRangeRequest> getAgeRange(HttpSession session) {
        return ResponseEntity.ok(signupService.getAgeRange(session));
    }

    @Operation(summary = "[뒤로가기] 성별 정보 조회")
    @GetMapping("/gender/data")
    public ResponseEntity<GenderRequest> getGender(HttpSession session) {
        return ResponseEntity.ok(signupService.getGender(session));
    }

    @Operation(summary = "[뒤로가기] 관심사 정보 조회 (Founder 전용)")
    @GetMapping("/interests/data")
    public ResponseEntity<InterestRequest> getInterests(HttpSession session) {
        return ResponseEntity.ok(signupService.getInterests(session));
    }

    @Operation(summary = "[뒤로가기] 예산 정보 조회 (Founder 전용)")
    @GetMapping("/budget/data")
    public ResponseEntity<BudgetRequest> getBudget(HttpSession session) {
        return ResponseEntity.ok(signupService.getBudget(session));
    }

    @Operation(summary = "[뒤로가기] 창업 경험 정보 조회 (Founder 전용)")
    @GetMapping("/experience/data")
    public ResponseEntity<ExperienceRequest> getExperience(HttpSession session) {
        return ResponseEntity.ok(signupService.getExperience(session));
    }

    // ---------------------------
    // 내부 DTO (프론트 호환)
    // ---------------------------

    /**
     * 프론트에서 기대하는 응답 형태:
     * { "JwtAccessToken": "..." }
     */
    @Data
    @AllArgsConstructor
    public static class SignupCompleteResponse {
        @com.fasterxml.jackson.annotation.JsonProperty("JwtAccessToken")
        private String jwtAccessToken;
    }

}
