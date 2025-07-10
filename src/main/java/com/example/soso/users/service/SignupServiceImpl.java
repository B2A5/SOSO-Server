package com.example.soso.users.service;

import static com.example.soso.global.config.CookieUtil.addRefreshTokenCookie;
import static com.example.soso.global.exception.domain.UserErrorCode.SESSION_NOT_VALID;
import static com.example.soso.global.exception.domain.UserErrorCode.STEPS_NOT_TYPE;

import com.example.soso.global.exception.util.UserAuthException;
import com.example.soso.jwt.JwtProperties;
import com.example.soso.jwt.JwtProvider;
import com.example.soso.jwt.JwtTokenDto;
import com.example.soso.jwt.RefreshTokenRedisService;
import com.example.soso.users.domain.dto.SignupSession;
import com.example.soso.users.domain.dto.TokenPair;
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
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignupServiceImpl implements SignupService {

    private static final String SESSION_KEY = "signup";
    private final UsersRepository usersRepository;
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenRedisService redisService;

    public SignupStep saveUserType(HttpSession session, UserType userType) {
        SignupSession signup = getValidatedSession(session);

        if (!SignupFlow.isFirstStep(SignupStep.USER_TYPE, userType)) {
            throw new IllegalStateException("잘못된 단계입니다.");
        }

        signup.setUserType(userType);
        SignupStep nextStep = SignupFlow.nextStep(userType, SignupStep.USER_TYPE);
        signup.setCurrentStep(nextStep);
        session.setAttribute(SESSION_KEY, signup);
        return nextStep;
    }


    public SignupStep saveRegion(HttpSession session, String regionId) {
        SignupSession signup = getValidatedSession(session);
        validateStep(signup, SignupStep.REGION);

        signup.setRegionId(regionId);
        SignupStep nextStep = SignupFlow.nextStep(signup.getUserType(), SignupStep.REGION);
        signup.setCurrentStep(nextStep);
        session.setAttribute(SESSION_KEY, signup);
        return nextStep;
    }

    public SignupStep saveAgeRange(HttpSession session, AgeRange ageRange) {
        SignupSession signup = getValidatedSession(session);
        validateStep(signup, SignupStep.AGE);

        signup.setAgeRange(ageRange);
        SignupStep nextStep = SignupFlow.nextStep(signup.getUserType(), SignupStep.AGE);
        signup.setCurrentStep(nextStep);
        session.setAttribute(SESSION_KEY, signup);
        return nextStep;
    }

    public SignupStep saveGender(HttpSession session, Gender gender) {
        SignupSession signup = getValidatedSession(session);
        validateStep(signup, SignupStep.GENDER);

        signup.setGender(gender);
        SignupStep nextStep = SignupFlow.nextStep(signup.getUserType(), SignupStep.GENDER);
        signup.setCurrentStep(nextStep);
        session.setAttribute(SESSION_KEY, signup);
        return nextStep;
    }

    public SignupStep saveInterests(HttpSession session, List<InterestType> interests) {
        SignupSession signup = getValidatedSession(session);
        validateStep(signup, SignupStep.INTERESTS);

        if (interests == null || interests.isEmpty()) {
            signup.setInterests(Collections.emptyList());
        } else {
            signup.setInterests(interests);
        }

        SignupStep nextStep = SignupFlow.nextStep(signup.getUserType(), SignupStep.INTERESTS);
        signup.setCurrentStep(nextStep);
        session.setAttribute(SESSION_KEY, signup);
        return nextStep;
    }

    public SignupStep saveBudget(HttpSession session, BudgetRange budget) {
        SignupSession signup = getValidatedSession(session);
        validateStep(signup, SignupStep.BUDGET);

        signup.setBudget(budget); // null일 경우도 허용
        SignupStep nextStep = SignupFlow.nextStep(signup.getUserType(), SignupStep.BUDGET);
        signup.setCurrentStep(nextStep);
        session.setAttribute(SESSION_KEY, signup);
        return nextStep;
    }

    public SignupStep saveExperience(HttpSession session, StartupExperience experience) {
        SignupSession signup = getValidatedSession(session);
        validateStep(signup, SignupStep.STARTUP);

        signup.setStartupExperience(experience);
        SignupStep nextStep = SignupFlow.nextStep(signup.getUserType(), SignupStep.STARTUP);
        signup.setCurrentStep(nextStep);
        session.setAttribute(SESSION_KEY, signup);
        return nextStep;
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
        signup.setCurrentStep(SignupStep.NINAME);
        session.setAttribute(SESSION_KEY, signup);
        return nickname;
    }

    public JwtTokenDto completeSignup(HttpSession session, HttpServletResponse response) {
        // 마지막 단계 검증 및 확인
        SignupSession signup = getValidatedSession(session);
        validateStep(signup, SignupStep.COMPLETE);

        // 유저 저장
        Users user = UserMapper.fromSignupSession(signup, signup.getUsername(), signup.getEmail(), signup.getProfileImageUrl());
        usersRepository.save(user);

        // 토큰 만들기
        TokenPair tokenPair = generateTokens(user.getId());

        // 레디스 와 httpOnly 쿠기 저장
        redisService.save(user.getId(), tokenPair.refreshToken(), jwtProperties.getRefreshTokenValidityInMs());
        addRefreshTokenCookie(response, tokenPair.refreshToken(), jwtProperties.getRefreshTokenValidityInMs());

        // 세션 삭제
        session.removeAttribute("signup");
        return new JwtTokenDto(tokenPair.accessToken());
    }

    private TokenPair generateTokens(String userId) {
        String accessToken = jwtProvider.generateAccessToken(userId);
        String refreshToken = jwtProvider.generateRefreshToken(userId);
        return new TokenPair(accessToken, refreshToken);
    }


    private void validateStep(SignupSession signup, SignupStep requestedStep) {
        if (signup == null || !SignupFlow.isValidNextStep(signup.getUserType(), signup.getCurrentStep(), requestedStep)) {
            throw new UserAuthException(STEPS_NOT_TYPE);
        }
    }

    private SignupSession getValidatedSession(HttpSession session) {
        SignupSession signup = (SignupSession) session.getAttribute(SESSION_KEY);

        if (signup == null) {
            throw new UserAuthException(SESSION_NOT_VALID);
        }
        return signup;
    }

}
