package com.example.soso.users.controller;

import com.example.soso.users.domain.dto.RegionRequest;
import com.example.soso.users.domain.dto.UserTypeRequest;
import com.example.soso.users.domain.entity.SignupStep;
import com.example.soso.users.service.SignupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Signup - Common", description = """
공통 회원가입 엔드포인트

1. /signup/user-type (유저 타입 선택 - 공통)
2. /signup/region (지역 선택 - 공통)
이후 타입별로 분기:
- INHABITANT: /signup/inhabitant/*
- FOUNDER: /signup/founder/*
""")
@RestController
@RequestMapping("/signup")
@RequiredArgsConstructor
public class SignupController {

    private final SignupService signupService;

    @Operation(summary = "[1단계] 유저 타입 설정 (공통)")
    @PostMapping("/user-type")
    public ResponseEntity<SignupStep> setUserType(@RequestBody @Valid UserTypeRequest request,
                                                  HttpSession session) {
        SignupStep nextStep = signupService.saveUserType(session, request.userType());
        return ResponseEntity.ok(nextStep);
    }

    @Operation(summary = "[2단계] 지역 설정 (공통)")
    @PostMapping("/region")
    public ResponseEntity<SignupStep> setRegion(@RequestBody @Valid RegionRequest request,
                                                HttpSession session) {
        SignupStep nextStep = signupService.saveRegion(session, request.regionId());
        return ResponseEntity.ok(nextStep);
    }
}