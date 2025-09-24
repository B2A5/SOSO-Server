package com.example.soso.users.util;

import com.example.soso.users.domain.entity.SignupStep;
import com.example.soso.users.domain.entity.UserType;
import java.util.EnumMap;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SignupFlow {

    private static final EnumMap<UserType, List<SignupStep>> FLOW = new EnumMap<>(UserType.class);

    static {
        FLOW.put(UserType.FOUNDER, List.of(
                SignupStep.USER_TYPE,
                SignupStep.REGION,
                SignupStep.AGE,
                SignupStep.GENDER,
                SignupStep.INTERESTS,
                SignupStep.BUDGET,
                SignupStep.STARTUP,
                SignupStep.NINAME,
                SignupStep.COMPLETE
        ));

        FLOW.put(UserType.INHABITANT, List.of(
                SignupStep.USER_TYPE,
                SignupStep.REGION,
                SignupStep.AGE,
                SignupStep.GENDER,
                SignupStep.NINAME,
                SignupStep.COMPLETE
        ));
    }

    public static boolean isValidNextStep(UserType userType, SignupStep currentStep, SignupStep requestedStep) {
        List<SignupStep> steps = FLOW.get(userType);

        if (userType == null || steps == null) {
            return false;
        }

        int currentIdx = steps.indexOf(currentStep);
        int requestedIdx = steps.indexOf(requestedStep);

        // 유효한 단계인지 확인
        if (currentIdx == -1 || requestedIdx == -1) {
            return false;
        }

        // 이미 완료된 단계(이전 단계) 또는 다음 단계로의 이동 허용
        // requestedIdx <= currentIdx + 1: 현재까지 완료된 단계 + 다음 단계까지 허용
        return requestedIdx <= currentIdx + 1;
    }

    private static List<SignupStep> getFlowSteps(UserType userType) {
        return FLOW.get(userType);
    }

    /**
     * 엄격한 단계 검증 - 처리 엔드포인트용
     * 현재 단계나 바로 다음 단계만 허용
     */
    public static boolean isValidProcessingStep(UserType userType, SignupStep currentStep, SignupStep requestedStep) {
        if (userType == null || requestedStep == null) {
            return false;
        }

        List<SignupStep> steps = getFlowSteps(userType);
        if (steps == null) {
            return false;
        }

        int currentIdx = steps.indexOf(currentStep);
        int requestedIdx = steps.indexOf(requestedStep);

        // 유효한 단계인지 확인
        if (requestedIdx == -1) {
            return false;
        }

        // currentStep이 null이면 첫 단계만 허용
        if (currentStep == null) {
            return requestedStep == SignupStep.USER_TYPE;
        }

        if (currentIdx == -1) {
            return false;
        }

        // 현재 단계이거나 바로 다음 단계만 허용
        return requestedIdx == currentIdx || requestedIdx == currentIdx + 1;
    }


    public static SignupStep nextStep(UserType userType, SignupStep currentStep) {
        if (userType == null || currentStep == null) {
            return null;
        }
        List<SignupStep> steps = FLOW.get(userType);
        if (steps == null) {
            return null;
        }
        int currentIdx = steps.indexOf(currentStep);
        return (currentIdx != -1 && currentIdx + 1 < steps.size()) ? steps.get(currentIdx + 1) : null;
    }

    public static boolean isFirstStep(SignupStep step, UserType userType) {
        if (userType == null || step == null) {
            return false;
        }
        List<SignupStep> steps = FLOW.get(userType);
        return steps != null && !steps.isEmpty() && steps.get(0) == step;
    }

    public static boolean isLastStep(SignupStep step, UserType userType) {
        if (userType == null || step == null) {
            return false;
        }
        List<SignupStep> steps = FLOW.get(userType);
        return steps != null && !steps.isEmpty() && steps.get(steps.size() - 1) == step;
    }

    public static SignupStep getPreviousStep(UserType userType, SignupStep currentStep) {
        if (userType == null || currentStep == null) {
            return null;
        }
        List<SignupStep> steps = FLOW.get(userType);
        if (steps == null) {
            return null;
        }
        int currentIdx = steps.indexOf(currentStep);
        return (currentIdx > 0) ? steps.get(currentIdx - 1) : null;
    }

    public static boolean canGoToPreviousStep(UserType userType, SignupStep currentStep) {
        if (userType == null || currentStep == null) {
            return false;
        }
        List<SignupStep> steps = FLOW.get(userType);
        if (steps == null) {
            return false;
        }
        int currentIdx = steps.indexOf(currentStep);
        return currentIdx > 0;
    }

    public static List<SignupStep> getCompletedSteps(UserType userType, SignupStep currentStep) {
        List<SignupStep> steps = FLOW.get(userType);
        if (steps == null || currentStep == null) {
            return List.of();
        }
        int currentIdx = steps.indexOf(currentStep);
        return (currentIdx >= 0) ? steps.subList(0, currentIdx + 1) : List.of();
    }
}
