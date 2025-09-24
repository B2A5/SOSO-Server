package com.example.soso.users.service;

import com.example.soso.global.exception.util.UserAuthException;
import com.example.soso.global.jwt.JwtProperties;
import com.example.soso.global.jwt.JwtProvider;
import com.example.soso.global.jwt.JwtTokenDto;
import com.example.soso.global.redis.RefreshTokenRedisRepository;
import com.example.soso.users.domain.dto.*;
import com.example.soso.users.domain.entity.*;
import com.example.soso.users.repository.UsersRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SignupService 단위 테스트")
class SignupServiceTest {

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private RefreshTokenRedisRepository redisService;

    @Mock
    private HttpServletResponse httpServletResponse;

    @InjectMocks
    private SignupServiceImpl signupService;

    private MockHttpSession mockSession;
    private SignupSession signupSession;

    @BeforeEach
    void setUp() {
        mockSession = new MockHttpSession();
        signupSession = createMockSignupSession();
        mockSession.setAttribute("signup", signupSession);
    }

    @Nested
    @DisplayName("사용자 타입 설정 테스트")
    class SaveUserTypeTest {

        @ParameterizedTest
        @EnumSource(UserType.class)
        @DisplayName("유효한 사용자 타입으로 설정 시 성공")
        void saveUserType_WithValidUserType_ShouldSucceed(UserType userType) {
            // given
            signupSession.setCurrentStep(null); // 초기 상태

            // when
            SignupStep result = signupService.saveUserType(mockSession, userType);

            // then
            assertThat(result).isEqualTo(SignupStep.REGION);
            assertThat(signupSession.getUserType()).isEqualTo(userType);
            assertThat(signupSession.getCurrentStep()).isEqualTo(SignupStep.REGION);
        }

        @Test
        @DisplayName("세션이 없을 때 예외 발생")
        void saveUserType_WithoutSession_ShouldThrowException() {
            // given
            MockHttpSession emptySession = new MockHttpSession();

            // when & then
            assertThatThrownBy(() -> signupService.saveUserType(emptySession, UserType.FOUNDER))
                    .isInstanceOf(UserAuthException.class);
        }
    }

    @Nested
    @DisplayName("지역 설정 테스트")
    class SaveRegionTest {

        @Test
        @DisplayName("유효한 지역 코드로 설정 시 성공")
        void saveRegion_WithValidRegionId_ShouldSucceed() {
            // given
            String regionId = "11010";
            signupSession.setUserType(UserType.FOUNDER);
            signupSession.setCurrentStep(SignupStep.REGION);

            // when
            SignupStep result = signupService.saveRegion(mockSession, regionId);

            // then
            assertThat(result).isEqualTo(SignupStep.AGE);
            assertThat(signupSession.getRegionId()).isEqualTo(regionId);
            assertThat(signupSession.getCurrentStep()).isEqualTo(SignupStep.AGE);
        }

        @Test
        @DisplayName("진행 중 뒤로가기로 인한 지역 재설정 허용")
        void saveRegion_AfterProgress_ShouldAllowBackNavigation() {
            // given
            signupSession.setUserType(UserType.FOUNDER);
            signupSession.setCurrentStep(SignupStep.GENDER); // 이미 AGE까지 완료된 상태라고 가정

            // when
            SignupStep next = signupService.saveRegion(mockSession, "22020");

            // then
            assertThat(next).isEqualTo(SignupStep.AGE);
            assertThat(signupSession.getRegionId()).isEqualTo("22020");
            assertThat(signupSession.getCurrentStep()).isEqualTo(SignupStep.AGE);
        }
    }

    @Nested
    @DisplayName("관심업종 설정 테스트")
    class SaveInterestsTest {

        @Test
        @DisplayName("관심업종 목록으로 설정 시 성공")
        void saveInterests_WithInterestsList_ShouldSucceed() {
            // given
            List<InterestType> interests = List.of(InterestType.MANUFACTURING, InterestType.ACCOMMODATION_FOOD);
            signupSession.setUserType(UserType.FOUNDER);
            signupSession.setCurrentStep(SignupStep.INTERESTS);

            // when
            SignupStep result = signupService.saveInterests(mockSession, interests);

            // then
            assertThat(result).isEqualTo(SignupStep.BUDGET);
            assertThat(signupSession.getInterests()).isEqualTo(interests);
            assertThat(signupSession.getCurrentStep()).isEqualTo(SignupStep.BUDGET);
        }

        @Test
        @DisplayName("빈 관심업종 목록으로 설정 시 성공")
        void saveInterests_WithEmptyList_ShouldSucceed() {
            // given
            signupSession.setUserType(UserType.FOUNDER);
            signupSession.setCurrentStep(SignupStep.INTERESTS);

            // when
            SignupStep result = signupService.saveInterests(mockSession, null);

            // then
            assertThat(result).isEqualTo(SignupStep.BUDGET);
            assertThat(signupSession.getInterests()).isEmpty();
        }

        @Test
        @DisplayName("INHABITANT가 관심업종 설정 시 예외 발생")
        void saveInterests_AsInhabitant_ShouldThrowException() {
            // given
            signupSession.setUserType(UserType.INHABITANT);
            signupSession.setCurrentStep(SignupStep.GENDER); // INHABITANT의 GENDER 다음 단계는 NICKNAME

            // when & then
            assertThatThrownBy(() -> signupService.saveInterests(mockSession, List.of(InterestType.OTHER)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("해당 사용자 유형에서 사용할 수 없는 단계입니다.");
        }
    }

    @Nested
    @DisplayName("예산 설정 테스트")
    class SaveBudgetTest {

        @Test
        @DisplayName("예산 설정 시 성공")
        void saveBudget_WithBudget_ShouldSucceed() {
            // given
            BudgetRange budget = BudgetRange.THOUSANDS_3000_5000;
            signupSession.setUserType(UserType.FOUNDER);
            signupSession.setCurrentStep(SignupStep.BUDGET);

            // when
            SignupStep result = signupService.saveBudget(mockSession, budget);

            // then
            assertThat(result).isEqualTo(SignupStep.STARTUP);
            assertThat(signupSession.getBudget()).isEqualTo(budget);
        }

        @Test
        @DisplayName("예산 건너뛰기(null) 시 성공")
        void saveBudget_WithNull_ShouldSucceed() {
            // given
            signupSession.setUserType(UserType.FOUNDER);
            signupSession.setCurrentStep(SignupStep.BUDGET);

            // when
            SignupStep result = signupService.saveBudget(mockSession, null);

            // then
            assertThat(result).isEqualTo(SignupStep.STARTUP);
            assertThat(signupSession.getBudget()).isNull();
        }
    }

    @Nested
    @DisplayName("닉네임 생성 테스트")
    class SaveNicknameTest {

        @Test
        @DisplayName("사용 가능한 닉네임 생성 성공")
        void saveNickname_WhenNicknameAvailable_ShouldSucceed() {
            // given
            signupSession.setUserType(UserType.FOUNDER);
            signupSession.setCurrentStep(SignupStep.NICKNAME);
            when(usersRepository.existsByNickname(anyString())).thenReturn(false);

            // when
            String result = signupService.saveNickname(mockSession);

            // then
            assertThat(result).isNotNull();
            assertThat(result).endsWith("문어");
            assertThat(signupSession.getNickname()).isEqualTo(result);
            verify(usersRepository).existsByNickname(result);
            assertThat(signupSession.getCurrentStep()).isEqualTo(SignupStep.COMPLETE);
        }

        @Test
        @DisplayName("기존 닉네임이 중복일 때 새 닉네임 생성")
        void saveNickname_WhenExistingNicknameDuplicated_ShouldGenerateNew() {
            // given
            signupSession.setUserType(UserType.FOUNDER);
            signupSession.setCurrentStep(SignupStep.NICKNAME);
            signupSession.setNickname("이미있는닉네임");

            when(usersRepository.existsByNickname("이미있는닉네임")).thenReturn(true);
            when(usersRepository.existsByNickname(argThat(nick -> nick.endsWith("문어")))).thenReturn(false);

            // when
            String result = signupService.saveNickname(mockSession);

            // then
            assertThat(result).isNotEqualTo("이미있는닉네임");
            assertThat(result).endsWith("문어");
            assertThat(signupSession.getNickname()).isEqualTo(result);
            assertThat(signupSession.getCurrentStep()).isEqualTo(SignupStep.COMPLETE);
        }
    }

    @Nested
    @DisplayName("회원가입 완료 테스트")
    class CompleteSignupTest {

        @Test
        @DisplayName("모든 단계 완료 후 회원가입 성공")
        void completeSignup_WhenAllStepsCompleted_ShouldSucceed() {
            // given
            signupSession.setUserType(UserType.FOUNDER);
            signupSession.setCurrentStep(SignupStep.COMPLETE);
            signupSession.setUsername("testUser");
            signupSession.setEmail("test@example.com");
            signupSession.setProfileImageUrl("https://example.com/profile.jpg");
            signupSession.setNickname("테스트문어");

            Users mockUser = mock(Users.class);
            when(usersRepository.save(any(Users.class))).thenReturn(mockUser);
            when(jwtProvider.generateAccessToken(any())).thenReturn("accessToken");
            when(jwtProvider.generateRefreshToken()).thenReturn("refreshToken");
            when(jwtProperties.getRefreshTokenValidityInMs()).thenReturn(1209600000L);

            // when
            JwtTokenDto result = signupService.completeSignup(mockSession, httpServletResponse);

            // then
            assertThat(result).isNotNull();
            assertThat(result.jwtAccessToken()).isEqualTo("accessToken");
            assertThat(mockSession.getAttribute("signup")).isNull(); // 세션 정리됨

            verify(usersRepository).save(any(Users.class));
            verify(redisService).save(any(), eq("refreshToken"), eq(1209600000L));
        }

        @Test
        @DisplayName("단계가 완료되지 않았을 때 예외 발생")
        void completeSignup_WhenStepsNotCompleted_ShouldThrowException() {
            // given
            signupSession.setUserType(UserType.FOUNDER);
            signupSession.setCurrentStep(SignupStep.BUDGET); // 완료되지 않은 단계

            // when & then
            assertThatThrownBy(() -> signupService.completeSignup(mockSession, httpServletResponse))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("다음 단계: budget");
        }
    }

    @Nested
    @DisplayName("데이터 조회 테스트")
    class GetDataTest {

        @Test
        @DisplayName("지역 정보 조회 성공")
        void getRegion_ShouldReturnRegionRequest() {
            // given
            signupSession.setRegionId("11010");

            // when
            RegionRequest result = signupService.getRegion(mockSession);

            // then
            assertThat(result.regionId()).isEqualTo("11010");
        }

        @Test
        @DisplayName("연령대 정보 조회 성공")
        void getAgeRange_ShouldReturnAgeRangeRequest() {
            // given
            signupSession.setAgeRange(AgeRange.TWENTIES);

            // when
            AgeRangeRequest result = signupService.getAgeRange(mockSession);

            // then
            assertThat(result.ageRange()).isEqualTo(AgeRange.TWENTIES);
        }

        @Test
        @DisplayName("관심업종 정보 조회 성공")
        void getInterests_ShouldReturnInterestRequest() {
            // given
            List<InterestType> interests = List.of(InterestType.MANUFACTURING);
            signupSession.setInterests(interests);

            // when
            InterestRequest result = signupService.getInterests(mockSession);

            // then
            assertThat(result.interests()).isEqualTo(interests);
        }

        @Test
        @DisplayName("세션이 없을 때 조회 시 예외 발생")
        void getData_WithoutSession_ShouldThrowException() {
            // given
            MockHttpSession emptySession = new MockHttpSession();

            // when & then
            assertThatThrownBy(() -> signupService.getRegion(emptySession))
                    .isInstanceOf(UserAuthException.class);
        }
    }

    private SignupSession createMockSignupSession() {
        SignupSession session = new SignupSession();
        session.setUsername("testUser");
        session.setEmail("test@example.com");
        session.setProfileImageUrl("https://example.com/profile.jpg");
        return session;
    }
}
