package com.example.soso.users.controller;

import com.example.soso.users.domain.dto.AgeRangeRequest;
import com.example.soso.users.domain.dto.GenderRequest;
import com.example.soso.users.domain.dto.RegionRequest;
import com.example.soso.users.domain.entity.UserType;
import com.example.soso.users.service.SignupService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/signup")
@RequiredArgsConstructor
public class SignupController {

    private final SignupService signupService;

    @PostMapping("/user-type")
    public ResponseEntity<Void> setUserType(@RequestBody @Valid UserType request,
                                            HttpSession session) {
        signupService.saveUserType(session, request);
        return ResponseEntity.ok().build();  // 또는 다음 단계 안내 응답
    }

    @PostMapping("/region")
    public ResponseEntity<Void> setRegion(@RequestBody @Valid RegionRequest request,
                                          HttpSession session) {
        signupService.saveRegion(session, request.regionId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/age-range")
    public ResponseEntity<Void> setAgeRange(@RequestBody @Valid AgeRangeRequest request,
                                            HttpSession session) {
        signupService.saveAgeRange(session, request.ageRange());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/gender")
    public ResponseEntity<Void> setGender(@RequestBody @Valid GenderRequest request,
                                          HttpSession session) {
        signupService.saveGender(session, request.gender());
        return ResponseEntity.ok().build();
    }
}
