package com.example.soso.users.util;

import com.example.soso.users.domain.entity.SignupStep;
import com.example.soso.users.domain.entity.UserType;
import java.util.EnumMap;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * 회원가입 단계 흐름표를 한 곳에서 관리한다.
 * 사용자 유형(예비 창업자/일반 거주민)에 따라 허용되는 단계가 다르기 때문에
 * 컨트롤러/서비스는 이 유틸에서 검증 로직과 다음 단계 정보를 가져간다.
 */
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
                SignupStep.NICKNAME,
                SignupStep.COMPLETE
        ));

        FLOW.put(UserType.INHABITANT, List.of(
                SignupStep.USER_TYPE,
                SignupStep.REGION,
                SignupStep.AGE,
                SignupStep.GENDER,
                SignupStep.NICKNAME,
                SignupStep.COMPLETE
        ));
    }

    /**
     * 기존 단계 검증 로직 (데이터 조회 등에서 사용)
     * 현재 단계에서 다음 단계로 이동 가능한지 여부를 확인한다.
     */
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

    /**
     * 사용자 유형에 해당하는 전체 단계 목록을 반환한다.
     */
    public static List<SignupStep> getFlow(UserType userType) {
        return FLOW.getOrDefault(userType, List.of());
    }

    private static List<SignupStep> getFlowSteps(UserType userType) {
        return FLOW.get(userType);
    }

    /**
     * 특정 단계가 해당 사용자 유형의 플로우에 포함되어 있는지 확인한다.
     */
    public static boolean isStepSupported(UserType userType, SignupStep step) {
        return getFlow(userType).contains(step);
    }

    public static int indexOf(UserType userType, SignupStep step) {
        return getFlow(userType).indexOf(step);
    }

    /**
     * 현재 플로우의 첫 단계.
     */
    public static SignupStep firstStep(UserType userType) {
        List<SignupStep> steps = getFlow(userType);
        return steps.isEmpty() ? null : steps.get(0);
    }

    public static SignupStep lastStep(UserType userType) {
        List<SignupStep> steps = getFlow(userType);
        return steps.isEmpty() ? null : steps.get(steps.size() - 1);
    }

    /**
     * 실제 API 요청을 처리할 때 사용하는 단계 검증 로직.
     *  - 이미 완료한 단계(뒤로가기) : 허용
     *  - 현재 기대 단계보다 앞서는 단계(건너뛰기) : 거부
     *  - 전체 플로우 완료 후 완료 단계 재호출 : 허용
     */
    public static boolean isValidProcessingStep(UserType userType, SignupStep currentStep, SignupStep requestedStep) {
        if (userType == null || requestedStep == null) {
            return false;
        }

        List<SignupStep> steps = getFlow(userType);
        if (steps.isEmpty()) {
            return false;
        }

        int requestedIdx = steps.indexOf(requestedStep);
        if (requestedIdx == -1) {
            return false;
        }

        if (currentStep == null) {
            // 모든 필수 단계가 완료된 상태. 마지막 단계 재진입은 허용
            SignupStep last = lastStep(userType);
            return requestedStep == last;
        }

        int currentIdx = steps.indexOf(currentStep);
        if (currentIdx == -1) {
            return false;
        }

        // 현재 기대 단계보다 이전 단계라면 허용 (뒤로가기 포함)
        if (requestedIdx <= currentIdx) {
            return true;
        }

        // 예상 단계보다 앞서는 경우는 허용하지 않음 (스킵 방지)
        return false;
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
