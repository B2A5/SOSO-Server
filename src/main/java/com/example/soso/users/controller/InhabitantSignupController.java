package com.example.soso.users.controller;

import com.example.soso.global.jwt.JwtTokenDto;
import com.example.soso.users.domain.dto.*;
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

@Tag(name = "Signup - Inhabitant", description = """
일반 거주민 회원가입 플로우

1. /signup/inhabitant/user-type  
2. /signup/inhabitant/region  
3. /signup/inhabitant/age-range  
4. /signup/inhabitant/gender  
5. /signup/inhabitant/nickname  
6. /signup/inhabitant/complete
""")
@RestController
@RequestMapping("/signup/inhabitant")
@RequiredArgsConstructor
public class InhabitantSignupController {

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

    @Operation(summary = "[5단계] 닉네임 생성")
    @PostMapping("/nickname")
    public ResponseEntity<String> saveNickname(HttpSession session) {
        String nickname = signupService.saveNiceName(session);
        return ResponseEntity.ok(nickname);
    }

    @Operation(summary = "[6단계] 회원가입 완료")
    @PostMapping("/complete")
    public ResponseEntity<JwtTokenDto> completeSignup(HttpSession session,
                                                      HttpServletResponse response) {
        JwtTokenDto jwtAccessToken = signupService.completeSignup(session, response);
        return ResponseEntity.ok(jwtAccessToken);
    }

    @Operation(summary = "[2단계 뒤로가기] 지역 정보 조회")
    @GetMapping("/region/data")
    public ResponseEntity<RegionRequest> getRegion(HttpSession session) {
        return ResponseEntity.ok(signupService.getRegion(session));
    }

    @Operation(summary = "[3단계 뒤로가기] 나이대 정보 조회")
    @GetMapping("/age-range/data")
    public ResponseEntity<AgeRangeRequest> getAgeRange(HttpSession session) {
        return ResponseEntity.ok(signupService.getAgeRange(session));
    }

    @Operation(summary = "[4단계 뒤로가기] 성별 정보 조회")
    @GetMapping("/gender/data")
    public ResponseEntity<GenderRequest> getGender(HttpSession session) {
        return ResponseEntity.ok(signupService.getGender(session));
    }

}
