package com.example.soso.users.service;

import static com.example.soso.global.config.CookieUtil.addRefreshTokenCookie;
import static com.example.soso.global.exception.domain.UserErrorCode.SESSION_NOT_VALID;
import static com.example.soso.global.exception.domain.UserErrorCode.STEPS_NOT_TYPE;

import com.example.soso.global.exception.util.UserAuthException;
import com.example.soso.global.jwt.JwtProperties;
import com.example.soso.global.jwt.JwtProvider;
import com.example.soso.global.jwt.JwtTokenDto;
import com.example.soso.global.redis.RefreshTokenRedisRepository;
import com.example.soso.users.domain.dto.AgeRangeRequest;
import com.example.soso.users.domain.dto.BudgetRequest;
import com.example.soso.users.domain.dto.ExperienceRequest;
import com.example.soso.users.domain.dto.GenderRequest;
import com.example.soso.users.domain.dto.RegionRequest;
import com.example.soso.users.domain.dto.SignupSession;
import com.example.soso.users.domain.dto.TokenPair;
import com.example.soso.users.domain.dto.UserMapper;
import com.example.soso.users.domain.entity.AgeRange;
import com.example.soso.users.domain.entity.BudgetRange;
import com.example.soso.users.domain.entity.Gender;
import com.example.soso.users.domain.entity.InterestRequest;
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
import java.util.function.Consumer;
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
    private final RefreshTokenRedisRepository redisService;

    public SignupStep saveUserType(HttpSession session, UserType userType) {
        SignupSession signup = getValidatedSession(session);
        // USER_TYPE은 첫 단계이므로 특별 처리
        if (!SignupFlow.isFirstStep(SignupStep.USER_TYPE, userType)) {
            log.warn("Invalid first step request: userType={}, sessionId={}",
                    userType,
                    session != null ? session.getId() : "null");
            throw new IllegalStateException("잘못된 단계입니다.");
        }

        signup.setUserType(userType);
        log.debug("User type set to: {}", userType);

        SignupStep nextStep = SignupFlow.nextStep(userType, SignupStep.USER_TYPE);
        signup.setCurrentStep(nextStep);
        session.setAttribute(SESSION_KEY, signup);

        log.info("Signup step processed: {} -> {}, UserType: {}", SignupStep.USER_TYPE, nextStep, userType);
        return nextStep;
    }

    public SignupStep saveRegion(HttpSession session, String regionId) {
        return processSignupStep(session, SignupStep.REGION, signup -> {
            signup.setRegionId(regionId);
            log.debug("Region set to: {}", regionId);
        });
    }

    public SignupStep saveAgeRange(HttpSession session, AgeRange ageRange) {
        return processSignupStep(session, SignupStep.AGE, signup -> {
            signup.setAgeRange(ageRange);
            log.debug("Age range set to: {}", ageRange);
        });
    }

    public SignupStep saveGender(HttpSession session, Gender gender) {
        return processSignupStep(session, SignupStep.GENDER, signup -> {
            signup.setGender(gender);
            log.debug("Gender set to: {}", gender);
        });
    }

    public SignupStep saveInterests(HttpSession session, List<InterestType> interests) {
        return processSignupStep(session, SignupStep.INTERESTS, signup -> {
            if (interests == null || interests.isEmpty()) {
                signup.setInterests(Collections.emptyList());
            } else {
                signup.setInterests(interests);
            }
            log.debug("Interests set to: {}", interests);
        });
    }

    public SignupStep saveBudget(HttpSession session, BudgetRange budget) {
        return processSignupStep(session, SignupStep.BUDGET, signup -> {
            signup.setBudget(budget); // null일 경우도 허용
            log.debug("Budget set to: {}", budget);
        });
    }

    public SignupStep saveExperience(HttpSession session, StartupExperience experience) {
        return processSignupStep(session, SignupStep.STARTUP, signup -> {
            signup.setStartupExperience(experience);
            log.debug("Experience set to: {}", experience);
        });
    }

    /**
     * 회원가입 단계 처리 템플릿 메서드
     * 공통 로직: 세션 검증 -> 단계 검증 -> 데이터 업데이트 -> 다음 단계 계산 -> 세션 저장
     */
    private SignupStep processSignupStep(HttpSession session, SignupStep currentStep, Consumer<SignupSession> updater) {
        SignupSession signup = getValidatedSession(session);
        validateStep(signup, currentStep);

        updater.accept(signup);

        SignupStep nextStep = SignupFlow.nextStep(signup.getUserType(), currentStep);
        signup.setCurrentStep(nextStep);
        session.setAttribute(SESSION_KEY, signup);

        log.info("Signup step processed: {} -> {}, UserType: {}", currentStep, nextStep, signup.getUserType());
        return nextStep;
    }

    public String saveNiceName(HttpSession session) {
        SignupSession signup = getValidatedSession(session);
        validateStep(signup, SignupStep.NINAME);

        String nickname = signup.getNickname();

        if (nickname == null || usersRepository.existsByNickname(nickname)) {
            nickname = RandomNicknameGenerator.generateUniqueNickname(usersRepository::existsByNickname);
        }

        signup.setNickname(nickname);
        signup.setCurrentStep(SignupStep.NINAME);
        session.setAttribute(SESSION_KEY, signup);

        log.info("Nickname generated and set: {}", nickname);
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
        String refreshToken = jwtProvider.generateRefreshToken();
        return new TokenPair(accessToken, refreshToken);
    }

    public RegionRequest getRegion(HttpSession session) {
        return getSignupData(session, signup -> new RegionRequest(signup.getRegionId()));
    }

    public AgeRangeRequest getAgeRange(HttpSession session) {
        return getSignupData(session, signup -> new AgeRangeRequest(signup.getAgeRange()));
    }

    public GenderRequest getGender(HttpSession session) {
        return getSignupData(session, signup -> new GenderRequest(signup.getGender()));
    }

    public InterestRequest getInterests(HttpSession session) {
        return getSignupData(session, signup -> new InterestRequest(signup.getInterests()));
    }

    public BudgetRequest getBudget(HttpSession session) {
        return getSignupData(session, signup -> new BudgetRequest(signup.getBudget()));
    }

    public ExperienceRequest getExperience(HttpSession session) {
        return getSignupData(session, signup -> new ExperienceRequest(signup.getStartupExperience()));
    }

    /**
     * 회원가입 데이터 조회 템플릿 메서드
     */
    private <T> T getSignupData(HttpSession session, java.util.function.Function<SignupSession, T> extractor) {
        SignupSession signup = getValidatedSession(session);
        return extractor.apply(signup);
    }


    private void validateStep(SignupSession signup, SignupStep requestedStep) {
        if (signup == null) {
            log.warn("Signup session is null during step validation for requestedStep={}", requestedStep);
            throw new UserAuthException(SESSION_NOT_VALID);
        }

        // For step processing, we use strict validation - only current or immediate next step
        // 한국어: 단계 처리의 경우 엄격한 검증을 사용합니다 - 현재 단계 또는 바로 다음 단계만 허용
        if (!SignupFlow.isValidProcessingStep(signup.getUserType(), signup.getCurrentStep(), requestedStep)) {
            log.warn("Signup step validation failed: userType={}, currentStep={}, requestedStep={}, email={}",
                    signup.getUserType(),
                    signup.getCurrentStep(),
                    requestedStep,
                    signup.getEmail());
            throw new UserAuthException(STEPS_NOT_TYPE);
        }
    }

    private SignupSession getValidatedSession(HttpSession session) {
        SignupSession signup = (SignupSession) session.getAttribute(SESSION_KEY);

        if (signup == null) {
            log.warn("Signup session missing or expired: sessionId={}", session != null ? session.getId() : "null");
            throw new UserAuthException(SESSION_NOT_VALID);
        }
        return signup;
    }

}
