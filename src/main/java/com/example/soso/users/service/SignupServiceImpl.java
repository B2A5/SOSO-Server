package com.example.soso.users.service;

import static com.example.soso.global.config.CookieUtil.addAccessTokenCookie;
import static com.example.soso.global.config.CookieUtil.addRefreshTokenCookie;
import static com.example.soso.global.exception.domain.UserErrorCode.SESSION_NOT_VALID;
import static com.example.soso.global.exception.domain.UserErrorCode.STEPS_NOT_TYPE;

import com.example.soso.global.exception.util.UserAuthException;
import com.example.soso.global.jwt.JwtProperties;
import com.example.soso.global.jwt.JwtProvider;
import com.example.soso.global.redis.RefreshTokenRedisRepository;
import com.example.soso.users.domain.dto.AgeRangeRequest;
import com.example.soso.users.domain.dto.BudgetRequest;
import com.example.soso.users.domain.dto.ExperienceRequest;
import com.example.soso.users.domain.dto.GenderRequest;
import com.example.soso.users.domain.dto.RegionRequest;
import com.example.soso.users.domain.dto.SignupCompleteResponse;
import com.example.soso.users.domain.dto.SignupSession;
import com.example.soso.users.domain.dto.TokenPair;
import com.example.soso.users.domain.dto.UserMapper;
import com.example.soso.users.domain.dto.UserResponse;
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

/**
 * 회원가입 플로우에서 세션을 기반으로 상태를 저장하고 검증하는 핵심 서비스.
 * 각 단계별 메서드는 공통 템플릿(processSignupStep)을 사용하여 중복을 줄이고,
 * 단계 검증은 SignupFlow 유틸을 통해 일관성 있게 처리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignupServiceImpl implements SignupService {

    private static final String SESSION_KEY = "signup";
    private final UsersRepository usersRepository;
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenRedisRepository redisService;
    private final UserMapper userMapper;

    /**
     * 1단계: 사용자 타입 저장. 첫 진입에서만 호출되며 이후 플로우가 결정된다.
     */
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

    /**
     * 2단계: 지역 정보 저장. 뒤로가기 시에도 다시 호출될 수 있다.
     */
    public SignupStep saveRegion(HttpSession session, String regionId) {
        return processSignupStep(session, SignupStep.REGION, signup -> {
            signup.setRegionId(regionId);
            log.debug("Region set to: {}", regionId);
        });
    }

    /**
     * 3단계: 연령대 저장.
     */
    public SignupStep saveAgeRange(HttpSession session, AgeRange ageRange) {
        return processSignupStep(session, SignupStep.AGE, signup -> {
            signup.setAgeRange(ageRange);
            log.debug("Age range set to: {}", ageRange);
        });
    }

    /**
     * 4단계: 성별 저장.
     */
    public SignupStep saveGender(HttpSession session, Gender gender) {
        return processSignupStep(session, SignupStep.GENDER, signup -> {
            signup.setGender(gender);
            log.debug("Gender set to: {}", gender);
        });
    }

    /**
     * 5단계: 관심 업종 저장 (예비 창업자 전용). 빈 리스트는 "선택 안 함"으로 처리한다.
     */
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

    /**
     * 6단계: 예산 구간 저장. null은 "건너뛰기" 의미로 허용한다.
     */
    public SignupStep saveBudget(HttpSession session, BudgetRange budget) {
        return processSignupStep(session, SignupStep.BUDGET, signup -> {
            signup.setBudget(budget); // null일 경우도 허용
            log.debug("Budget set to: {}", budget);
        });
    }

    /**
     * 7단계: 창업 경험 유무 저장.
     */
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

    /**
     * 8단계: 닉네임 생성. 이미 동일 닉네임이 존재하면 새로운 닉네임을 만들어준다.
     * 생성 후 다음 단계(완료 단계)로 현재 스텝을 갱신한다.
     */
    public String saveNickname(HttpSession session) {
        SignupSession signup = getValidatedSession(session);
        validateStep(signup, SignupStep.NICKNAME);

        String nickname = signup.getNickname();

        if (nickname == null || usersRepository.existsByNickname(nickname)) {
            nickname = RandomNicknameGenerator.generateUniqueNickname(usersRepository::existsByNickname);
        }

        signup.setNickname(nickname);

        SignupStep nextStep = SignupFlow.nextStep(signup.getUserType(), SignupStep.NICKNAME);
        signup.setCurrentStep(nextStep);
        session.setAttribute(SESSION_KEY, signup);

        log.info("Nickname generated and set: {}", nickname);
        return nickname;
    }

    /**
     * 9단계: 회원가입 완료.
     * 저장된 세션 정보를 기반으로 Users 엔터티를 생성하고 토큰을 발급한다.
     */
    public SignupCompleteResponse completeSignup(HttpSession session, HttpServletResponse response) {
        // 마지막 단계 검증 및 확인
        SignupSession signup = getValidatedSession(session);
        validateStep(signup, SignupStep.COMPLETE);

        // 유저 저장
        Users user = userMapper.fromSignupSession(signup, signup.getUsername(), signup.getEmail(), signup.getProfileImageUrl());
        user = usersRepository.save(user);  // save()가 ID가 할당된 엔티티를 반환

        // 토큰 만들기
        TokenPair tokenPair = generateTokens(user.getId());

        // 레디스 와 httpOnly 쿠기 저장
        redisService.saveByUserId(user.getId(), tokenPair.refreshToken(), jwtProperties.getRefreshTokenValidityInMs());

        // 쿠키에 토큰 설정 (SSR 지원)
        addAccessTokenCookie(response, tokenPair.accessToken(), jwtProperties.getAccessTokenValidityInMs());
        addRefreshTokenCookie(response, tokenPair.refreshToken(), jwtProperties.getRefreshTokenValidityInMs());

        // UserResponse 생성
        UserResponse userResponse = userMapper.toUserResponse(user);

        // 세션 삭제
        session.removeAttribute("signup");
        return new SignupCompleteResponse(tokenPair.accessToken(), userResponse);
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
    /**
     * 조회 계열 API에서 공통으로 사용하는 세션 래핑 함수.
     */
    private <T> T getSignupData(HttpSession session, java.util.function.Function<SignupSession, T> extractor) {
        SignupSession signup = getValidatedSession(session);
        return extractor.apply(signup);
    }


    /**
     * 현재 단계가 플로우 상 유효한지 검증한다.
     *  - 세션 없음 → UserAuthException (401)
     *  - 사용자 유형에서 지원하지 않는 단계 → IllegalArgumentException
     *  - 건너뛰기 → IllegalArgumentException("다음 단계: ...")
     */
    private void validateStep(SignupSession signup, SignupStep requestedStep) {
        if (signup == null) {
            log.warn("Signup session is null during step validation for requestedStep={}", requestedStep);
            throw new UserAuthException(SESSION_NOT_VALID);
        }

        // For step processing, we use strict validation - only current or immediate next step
        // 한국어: 단계 처리의 경우 엄격한 검증을 사용합니다 - 현재 단계 또는 바로 다음 단계만 허용
        UserType userType = signup.getUserType();

        if (!SignupFlow.isStepSupported(userType, requestedStep)) {
            log.warn("Signup step unsupported for userType: userType={}, requestedStep={}, email={}",
                    userType,
                    requestedStep,
                    signup.getEmail());
            throw new IllegalArgumentException("해당 사용자 유형에서 사용할 수 없는 단계입니다.");
        }

        SignupStep currentExpected = signup.getCurrentStep();

        if (SignupFlow.isValidProcessingStep(userType, currentExpected, requestedStep)) {
            return;
        }

        SignupStep nextStep = currentExpected != null ? currentExpected : SignupStep.COMPLETE;

        log.warn("Signup step validation failed: userType={}, currentStep={}, requestedStep={}, email={}",
                userType,
                currentExpected,
                requestedStep,
                signup.getEmail());

        throw new IllegalArgumentException("다음 단계: " + nextStep.name().toLowerCase());
    }

    /**
     * 세션에서 SignupSession을 꺼내는 공용 메서드.
     */
    private SignupSession getValidatedSession(HttpSession session) {
        SignupSession signup = (SignupSession) session.getAttribute(SESSION_KEY);

        if (signup == null) {
            log.warn("Signup session missing or expired: sessionId={}", session != null ? session.getId() : "null");
            throw new UserAuthException(SESSION_NOT_VALID);
        }
        return signup;
    }

}
