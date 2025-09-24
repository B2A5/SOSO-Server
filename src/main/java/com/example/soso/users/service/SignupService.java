package com.example.soso.users.service;

import com.example.soso.global.jwt.JwtTokenDto;
import com.example.soso.users.domain.dto.AgeRangeRequest;
import com.example.soso.users.domain.dto.BudgetRequest;
import com.example.soso.users.domain.dto.ExperienceRequest;
import com.example.soso.users.domain.dto.GenderRequest;
import com.example.soso.users.domain.dto.RegionRequest;
import com.example.soso.users.domain.entity.AgeRange;
import com.example.soso.users.domain.entity.BudgetRange;
import com.example.soso.users.domain.entity.Gender;
import com.example.soso.users.domain.entity.InterestRequest;
import com.example.soso.users.domain.entity.InterestType;
import com.example.soso.users.domain.entity.SignupStep;
import com.example.soso.users.domain.entity.StartupExperience;
import com.example.soso.users.domain.entity.UserType;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.List;

public interface SignupService {

    SignupStep saveUserType(HttpSession session, UserType userType);

    SignupStep saveRegion(HttpSession session, String regionId);

    SignupStep saveAgeRange(HttpSession session, AgeRange ageRange);

    SignupStep saveGender(HttpSession session, Gender gender);

    SignupStep saveInterests(HttpSession session, List<InterestType> interests);

    SignupStep saveBudget(HttpSession session, BudgetRange budget);

    SignupStep saveExperience(HttpSession session, StartupExperience experience);

    String saveNickname(HttpSession session);

    JwtTokenDto completeSignup(HttpSession session, HttpServletResponse response);

    // 조회 메서드 (뒤로가기 용)
    RegionRequest getRegion(HttpSession session);
    AgeRangeRequest getAgeRange(HttpSession session);
    GenderRequest getGender(HttpSession session);
    InterestRequest getInterests(HttpSession session);
    BudgetRequest getBudget(HttpSession session);
    ExperienceRequest getExperience(HttpSession session);
}
