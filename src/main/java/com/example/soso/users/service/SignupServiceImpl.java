package com.example.soso.users.service;

import com.example.soso.users.domain.dto.SignupSession;
import com.example.soso.users.domain.entity.AgeRange;
import com.example.soso.users.domain.entity.SignupStep;
import com.example.soso.users.domain.entity.UserType;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

@Service
public class SignupServiceImpl implements SignupService {

    private static final String SESSION_KEY = "signup";

    public void saveUserType(HttpSession session, UserType userType) {
        SignupSession signup = (SignupSession) session.getAttribute(SESSION_KEY);

        if (signup == null) {
            signup = new SignupSession();
        }

        if (signup.getCurrentStep() != SignupStep.USER_TYPE) {
            throw new IllegalStateException("순서가 잘못되었습니다.");
        }
        signup.setUserType(userType);
        signup.setCurrentStep(SignupStep.USER_TYPE);
        session.setAttribute(SESSION_KEY, signup);
    }

    public void saveRegion(HttpSession session, String regionId) {
        SignupSession signup = (SignupSession) session.getAttribute(SESSION_KEY);

        if (signup == null || signup.getCurrentStep() != SignupStep.REGION) {
            throw new IllegalStateException("잘못된 요청 순서입니다.");
        }

        signup.setRegionId(regionId);
        signup.setCurrentStep(SignupStep.REGION);
        session.setAttribute(SESSION_KEY, signup);
    }

    public void saveAgeRange(HttpSession session, AgeRange ageRange) {
        SignupSession signup = (SignupSession) session.getAttribute(SESSION_KEY);

        if (signup == null || signup.getCurrentStep() != SignupStep.GENDER) {
            throw new IllegalStateException("잘못된 요청 순서입니다.");
        }

        signup.setAgeRange(ageRange);
        signup.setCurrentStep(SignupStep.GENDER);
        session.setAttribute(SESSION_KEY, signup);
    }
}
