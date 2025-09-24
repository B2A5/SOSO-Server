package com.example.soso.users.util;

import com.example.soso.users.domain.entity.SignupStep;
import com.example.soso.users.domain.entity.UserType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("회원가입 플로우 유틸리티 테스트")
class SignupFlowTest {

    @Test
    @DisplayName("INHABITANT 플로우 단계 순서 확인")
    void inhabitantFlowStepsOrder() {
        // when
        List<SignupStep> expectedSteps = List.of(
                SignupStep.USER_TYPE,
                SignupStep.REGION,
                SignupStep.AGE,
                SignupStep.GENDER,
                SignupStep.NINAME,
                SignupStep.COMPLETE
        );

        // then
        assertThat(SignupFlow.nextStep(UserType.INHABITANT, SignupStep.USER_TYPE))
                .isEqualTo(SignupStep.REGION);
        assertThat(SignupFlow.nextStep(UserType.INHABITANT, SignupStep.REGION))
                .isEqualTo(SignupStep.AGE);
        assertThat(SignupFlow.nextStep(UserType.INHABITANT, SignupStep.AGE))
                .isEqualTo(SignupStep.GENDER);
        assertThat(SignupFlow.nextStep(UserType.INHABITANT, SignupStep.GENDER))
                .isEqualTo(SignupStep.NINAME);
        assertThat(SignupFlow.nextStep(UserType.INHABITANT, SignupStep.NINAME))
                .isEqualTo(SignupStep.COMPLETE);
        assertThat(SignupFlow.nextStep(UserType.INHABITANT, SignupStep.COMPLETE))
                .isNull();
    }

    @Test
    @DisplayName("FOUNDER 플로우 단계 순서 확인")
    void founderFlowStepsOrder() {
        // then
        assertThat(SignupFlow.nextStep(UserType.FOUNDER, SignupStep.USER_TYPE))
                .isEqualTo(SignupStep.REGION);
        assertThat(SignupFlow.nextStep(UserType.FOUNDER, SignupStep.REGION))
                .isEqualTo(SignupStep.AGE);
        assertThat(SignupFlow.nextStep(UserType.FOUNDER, SignupStep.AGE))
                .isEqualTo(SignupStep.GENDER);
        assertThat(SignupFlow.nextStep(UserType.FOUNDER, SignupStep.GENDER))
                .isEqualTo(SignupStep.INTERESTS);
        assertThat(SignupFlow.nextStep(UserType.FOUNDER, SignupStep.INTERESTS))
                .isEqualTo(SignupStep.BUDGET);
        assertThat(SignupFlow.nextStep(UserType.FOUNDER, SignupStep.BUDGET))
                .isEqualTo(SignupStep.STARTUP);
        assertThat(SignupFlow.nextStep(UserType.FOUNDER, SignupStep.STARTUP))
                .isEqualTo(SignupStep.NINAME);
        assertThat(SignupFlow.nextStep(UserType.FOUNDER, SignupStep.NINAME))
                .isEqualTo(SignupStep.COMPLETE);
        assertThat(SignupFlow.nextStep(UserType.FOUNDER, SignupStep.COMPLETE))
                .isNull();
    }

    @ParameterizedTest
    @MethodSource("validNextStepTestData")
    @DisplayName("유효한 다음 단계 이동 테스트 - 정방향 및 역방향 네비게이션")
    void isValidNextStep_ForwardAndBackwardNavigation(UserType userType, SignupStep currentStep, SignupStep requestedStep, boolean expected) {
        // when & then
        assertThat(SignupFlow.isValidNextStep(userType, currentStep, requestedStep))
                .isEqualTo(expected);
    }

    private static Stream<Arguments> validNextStepTestData() {
        return Stream.of(
                // INHABITANT 정방향 네비게이션
                Arguments.of(UserType.INHABITANT, SignupStep.USER_TYPE, SignupStep.REGION, true),
                Arguments.of(UserType.INHABITANT, SignupStep.REGION, SignupStep.AGE, true),
                Arguments.of(UserType.INHABITANT, SignupStep.AGE, SignupStep.GENDER, true),
                Arguments.of(UserType.INHABITANT, SignupStep.GENDER, SignupStep.NINAME, true),

                // INHABITANT 역방향 네비게이션 (허용)
                Arguments.of(UserType.INHABITANT, SignupStep.AGE, SignupStep.USER_TYPE, true),
                Arguments.of(UserType.INHABITANT, SignupStep.GENDER, SignupStep.REGION, true),
                Arguments.of(UserType.INHABITANT, SignupStep.NINAME, SignupStep.AGE, true),

                // INHABITANT 현재 단계 유지 (허용)
                Arguments.of(UserType.INHABITANT, SignupStep.AGE, SignupStep.AGE, true),

                // INHABITANT 건너뛰기 (불허용)
                Arguments.of(UserType.INHABITANT, SignupStep.USER_TYPE, SignupStep.AGE, false),
                Arguments.of(UserType.INHABITANT, SignupStep.REGION, SignupStep.GENDER, false),

                // FOUNDER 정방향 네비게이션
                Arguments.of(UserType.FOUNDER, SignupStep.USER_TYPE, SignupStep.REGION, true),
                Arguments.of(UserType.FOUNDER, SignupStep.GENDER, SignupStep.INTERESTS, true),
                Arguments.of(UserType.FOUNDER, SignupStep.INTERESTS, SignupStep.BUDGET, true),
                Arguments.of(UserType.FOUNDER, SignupStep.BUDGET, SignupStep.STARTUP, true),

                // FOUNDER 역방향 네비게이션 (허용)
                Arguments.of(UserType.FOUNDER, SignupStep.INTERESTS, SignupStep.USER_TYPE, true),
                Arguments.of(UserType.FOUNDER, SignupStep.STARTUP, SignupStep.BUDGET, true),

                // FOUNDER 건너뛰기 (불허용)
                Arguments.of(UserType.FOUNDER, SignupStep.USER_TYPE, SignupStep.GENDER, false),
                Arguments.of(UserType.FOUNDER, SignupStep.REGION, SignupStep.INTERESTS, false),

                // 잘못된 유저타입
                Arguments.of(null, SignupStep.USER_TYPE, SignupStep.REGION, false),

                // INHABITANT에게 FOUNDER 단계 요청 (불허용)
                Arguments.of(UserType.INHABITANT, SignupStep.GENDER, SignupStep.INTERESTS, false),
                Arguments.of(UserType.INHABITANT, SignupStep.GENDER, SignupStep.BUDGET, false)
        );
    }

    @Test
    @DisplayName("첫 번째 단계 확인")
    void isFirstStep() {
        assertThat(SignupFlow.isFirstStep(SignupStep.USER_TYPE, UserType.INHABITANT)).isTrue();
        assertThat(SignupFlow.isFirstStep(SignupStep.USER_TYPE, UserType.FOUNDER)).isTrue();
        assertThat(SignupFlow.isFirstStep(SignupStep.REGION, UserType.INHABITANT)).isFalse();
        assertThat(SignupFlow.isFirstStep(SignupStep.REGION, UserType.FOUNDER)).isFalse();
    }

    @Test
    @DisplayName("마지막 단계 확인")
    void isLastStep() {
        assertThat(SignupFlow.isLastStep(SignupStep.COMPLETE, UserType.INHABITANT)).isTrue();
        assertThat(SignupFlow.isLastStep(SignupStep.COMPLETE, UserType.FOUNDER)).isTrue();
        assertThat(SignupFlow.isLastStep(SignupStep.NINAME, UserType.INHABITANT)).isFalse();
        assertThat(SignupFlow.isLastStep(SignupStep.NINAME, UserType.FOUNDER)).isFalse();
    }

    @Test
    @DisplayName("이전 단계 조회")
    void getPreviousStep() {
        assertThat(SignupFlow.getPreviousStep(UserType.INHABITANT, SignupStep.REGION))
                .isEqualTo(SignupStep.USER_TYPE);
        assertThat(SignupFlow.getPreviousStep(UserType.INHABITANT, SignupStep.AGE))
                .isEqualTo(SignupStep.REGION);
        assertThat(SignupFlow.getPreviousStep(UserType.INHABITANT, SignupStep.USER_TYPE))
                .isNull();

        assertThat(SignupFlow.getPreviousStep(UserType.FOUNDER, SignupStep.INTERESTS))
                .isEqualTo(SignupStep.GENDER);
        assertThat(SignupFlow.getPreviousStep(UserType.FOUNDER, SignupStep.BUDGET))
                .isEqualTo(SignupStep.INTERESTS);
    }

    @Test
    @DisplayName("이전 단계로 이동 가능 여부 확인")
    void canGoToPreviousStep() {
        assertThat(SignupFlow.canGoToPreviousStep(UserType.INHABITANT, SignupStep.USER_TYPE)).isFalse();
        assertThat(SignupFlow.canGoToPreviousStep(UserType.INHABITANT, SignupStep.REGION)).isTrue();
        assertThat(SignupFlow.canGoToPreviousStep(UserType.INHABITANT, SignupStep.AGE)).isTrue();

        assertThat(SignupFlow.canGoToPreviousStep(UserType.FOUNDER, SignupStep.USER_TYPE)).isFalse();
        assertThat(SignupFlow.canGoToPreviousStep(UserType.FOUNDER, SignupStep.INTERESTS)).isTrue();
    }

    @Test
    @DisplayName("완료된 단계들 조회")
    void getCompletedSteps() {
        List<SignupStep> inhabitantCompletedSteps = SignupFlow.getCompletedSteps(UserType.INHABITANT, SignupStep.AGE);
        assertThat(inhabitantCompletedSteps).containsExactly(
                SignupStep.USER_TYPE,
                SignupStep.REGION,
                SignupStep.AGE
        );

        List<SignupStep> founderCompletedSteps = SignupFlow.getCompletedSteps(UserType.FOUNDER, SignupStep.INTERESTS);
        assertThat(founderCompletedSteps).containsExactly(
                SignupStep.USER_TYPE,
                SignupStep.REGION,
                SignupStep.AGE,
                SignupStep.GENDER,
                SignupStep.INTERESTS
        );

        List<SignupStep> emptySteps = SignupFlow.getCompletedSteps(UserType.INHABITANT, null);
        assertThat(emptySteps).isEmpty();
    }
}