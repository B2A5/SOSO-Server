package com.example.soso.users.service;

import com.example.soso.users.domain.dto.SignupSession;
import com.example.soso.users.domain.dto.UserMapper;
import com.example.soso.users.domain.entity.AgeRange;
import com.example.soso.users.domain.entity.BudgetRange;
import com.example.soso.users.domain.entity.Gender;
import com.example.soso.users.domain.entity.InterestType;
import com.example.soso.users.domain.entity.SignupStep;
import com.example.soso.users.domain.entity.StartupExperience;
import com.example.soso.users.domain.entity.UserType;
import com.example.soso.users.domain.entity.Users;
import com.example.soso.users.repository.UsersRepository;
import com.example.soso.users.util.RandomNicknameGenerator;
import com.example.soso.users.util.SignupFlow;
import jakarta.servlet.http.HttpSession;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SignupServiceImpl implements SignupService {

    private static final String SESSION_KEY = "signup";
    private final UsersRepository usersRepository;

    public void saveUserType(HttpSession session, UserType userType) {
        SignupSession signup = getValidatedSession(session);

        if (!SignupFlow.isFirstStep(SignupStep.USER_TYPE, userType)) {
            throw new IllegalStateException("잘못된 단계입니다.");
        }

        signup.setUserType(userType);
        signup.setCurrentStep(SignupFlow.nextStep(userType, SignupStep.USER_TYPE));
        session.setAttribute(SESSION_KEY, signup);
    }

    public void saveRegion(HttpSession session, String regionId) {
        SignupSession signup = getValidatedSession(session);
        validateStep(signup, SignupStep.REGION);

        signup.setRegionId(regionId);
        signup.setCurrentStep(SignupFlow.nextStep(signup.getUserType(), SignupStep.REGION));
        session.setAttribute(SESSION_KEY, signup);
    }

    public void saveAgeRange(HttpSession session, AgeRange ageRange) {
        SignupSession signup = getValidatedSession(session);
        validateStep(signup, SignupStep.AGE);

        signup.setAgeRange(ageRange);
        signup.setCurrentStep(SignupFlow.nextStep(signup.getUserType(), SignupStep.AGE));
        session.setAttribute(SESSION_KEY, signup);
    }

    public void saveGender(HttpSession session, Gender gender) {
        SignupSession signup = getValidatedSession(session);
        validateStep(signup, SignupStep.GENDER);

        signup.setGender(gender);
        signup.setCurrentStep(SignupFlow.nextStep(signup.getUserType(), SignupStep.GENDER));
        session.setAttribute(SESSION_KEY, signup);
    }

    public void saveInterests(HttpSession session, List<InterestType> interests) {
        SignupSession signup = getValidatedSession(session);
        validateStep(signup, SignupStep.INTERESTS);

        if (interests == null || interests.isEmpty()) {
            signup.setInterests(Collections.emptyList());
        } else {
            signup.setInterests(interests);
        }

        signup.setCurrentStep(SignupFlow.nextStep(signup.getUserType(), SignupStep.INTERESTS));
        session.setAttribute(SESSION_KEY, signup);
    }

    public void saveBudget(HttpSession session, BudgetRange budget) {
        SignupSession signup = getValidatedSession(session);
        validateStep(signup, SignupStep.BUDGET);

        signup.setBudget(budget); // null일 경우도 허용
        signup.setCurrentStep(SignupFlow.nextStep(signup.getUserType(), SignupStep.BUDGET));
        session.setAttribute(SESSION_KEY, signup);
    }

    public void saveExperience(HttpSession session, StartupExperience experience) {
        SignupSession signup = getValidatedSession(session);
        validateStep(signup, SignupStep.STARTUP);

        signup.setStartupExperience(experience);
        signup.setCurrentStep(SignupStep.STARTUP);
        session.setAttribute(SESSION_KEY, signup);
    }

    public String saveNiceName(HttpSession session){
        SignupSession signup = getValidatedSession(session);
        validateStep(signup, SignupStep.NINAME);

        Set<String> takenNicknames = usersRepository.findAllNicknames();
        String nickname = signup.getNickname();

        if (nickname == null || takenNicknames.contains(nickname)) {
            nickname = RandomNicknameGenerator.generateUniqueNickname(takenNicknames);
            signup.setNickname(nickname);
        }

        signup.setNickname(nickname);
        signup.setCurrentStep(SignupStep.STARTUP);
        session.setAttribute(SESSION_KEY, signup);
        return nickname;
    }

    public void completeSignup(HttpSession session) {
        SignupSession signup = getValidatedSession(session);
        if (signup.getCurrentStep() != SignupStep.COMPLETE) {
            throw new IllegalStateException("아직 모든 단계를 완료하지 않았습니다.");
        }

        Users user = UserMapper.fromSignupSession(signup, signup.getUsername(), signup.getEmail(), signup.getProfileImageUrl());
        usersRepository.save(user);
        session.removeAttribute("signup");
    }



    private void validateStep(SignupSession signup, SignupStep requestedStep) {
        if (signup == null || !SignupFlow.isValidNextStep(signup.getUserType(), signup.getCurrentStep(), requestedStep)) {
            throw new IllegalStateException("현재 사용자 유형에 맞지 않는 단계입니다.");
        }
    }

    private SignupSession getValidatedSession(HttpSession session) {
        SignupSession signup = (SignupSession) session.getAttribute(SESSION_KEY);

        if (signup == null) {
            throw new IllegalStateException("회원가입 세션이 존재하지 않습니다.");
        }
        return signup;
    }

}
