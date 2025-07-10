package com.example.soso.users.controller;

import com.example.soso.jwt.JwtProvider;
import com.example.soso.jwt.JwtTokenDto;
import com.example.soso.users.domain.dto.*;
import com.example.soso.users.domain.entity.InterestRequest;
import com.example.soso.users.domain.entity.SignupStep;
import com.example.soso.users.service.SignupService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/signup")
@RequiredArgsConstructor
public class SignupController {

    private final SignupService signupService;

    @PostMapping("/start")
    public ResponseEntity<Void> startSignup(HttpSession session) {
        SignupSession signup = new SignupSession();
        signup.setCurrentStep(SignupStep.USER_TYPE);
        session.setAttribute("signup", signup);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/user-type")
    public ResponseEntity<SignupStep> setUserType(@RequestBody @Valid UserTypeRequest request,
                                                  HttpSession session) {
        SignupStep nextStep = signupService.saveUserType(session, request.userType());
        return ResponseEntity.ok(nextStep);
    }

    @PostMapping("/region")
    public ResponseEntity<SignupStep> setRegion(@RequestBody @Valid RegionRequest request,
                                                HttpSession session) {
        SignupStep nextStep = signupService.saveRegion(session, request.regionId());
        return ResponseEntity.ok(nextStep);
    }

    @PostMapping("/age-range")
    public ResponseEntity<SignupStep> setAgeRange(@RequestBody @Valid AgeRangeRequest request,
                                                  HttpSession session) {
        SignupStep nextStep = signupService.saveAgeRange(session, request.ageRange());
        return ResponseEntity.ok(nextStep);
    }

    @PostMapping("/gender")
    public ResponseEntity<SignupStep> setGender(@RequestBody @Valid GenderRequest request,
                                                HttpSession session) {
        SignupStep nextStep = signupService.saveGender(session, request.gender());
        return ResponseEntity.ok(nextStep);
    }

    @PostMapping("/interests")
    public ResponseEntity<SignupStep> setInterests(@RequestBody @Valid InterestRequest request,
                                                   HttpSession session) {
        SignupStep nextStep = signupService.saveInterests(session, request.interests());
        return ResponseEntity.ok(nextStep);
    }

    @PostMapping("/budget")
    public ResponseEntity<SignupStep> setBudget(@RequestBody BudgetRequest request, HttpSession session) {
        SignupStep nextStep = signupService.saveBudget(session, request.budget());
        return ResponseEntity.ok(nextStep);
    }

    @PostMapping("/experience")
    public ResponseEntity<SignupStep> setExperience(@RequestBody @Valid ExperienceRequest request,
                                                    HttpSession session) {
        SignupStep nextStep = signupService.saveExperience(session, request.experience());
        return ResponseEntity.ok(nextStep);
    }

    @PostMapping("/nickname")
    public ResponseEntity<String> saveNickname(HttpSession session) {
        String nextStep = signupService.saveNiceName(session);
        return ResponseEntity.ok(nextStep);
    }

    @PostMapping("/complete")
    public ResponseEntity<JwtTokenDto> completeSignup(HttpSession session) {
        JwtTokenDto jwtAccessToken = signupService.completeSignup(session);
        return ResponseEntity.ok(jwtAccessToken);
    }

}
