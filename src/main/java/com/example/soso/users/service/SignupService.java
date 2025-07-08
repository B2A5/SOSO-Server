package com.example.soso.users.service;

import com.example.soso.users.domain.entity.AgeRange;
import com.example.soso.users.domain.entity.BudgetRange;
import com.example.soso.users.domain.entity.Gender;
import com.example.soso.users.domain.entity.InterestType;
import com.example.soso.users.domain.entity.StartupExperience;
import com.example.soso.users.domain.entity.UserType;
import jakarta.servlet.http.HttpSession;
import java.util.List;

public interface SignupService {

    void saveUserType(HttpSession session, UserType userType);

    void saveRegion(HttpSession session, String regionId);

    void saveAgeRange(HttpSession session, AgeRange ageRange);

    void saveGender(HttpSession session, Gender gender);

    void saveInterests(HttpSession session, List<InterestType> interests);

    void saveBudget(HttpSession session, BudgetRange budget);

    void saveExperience(HttpSession session, StartupExperience experience);

    String saveNiceName(HttpSession session);

    void completeSignup(HttpSession session);
}
