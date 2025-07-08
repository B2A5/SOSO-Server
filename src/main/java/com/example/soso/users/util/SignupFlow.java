package com.example.soso.users.util;

import com.example.soso.users.domain.entity.SignupStep;
import com.example.soso.users.domain.entity.UserType;
import java.util.EnumMap;
import java.util.List;

public class SignupFlow {

    private static final EnumMap<UserType, List<SignupStep>> FLOW = new EnumMap<>(UserType.class);

    static {
        FLOW.put(UserType.FOUNDER, List.of(
                SignupStep.USER_TYPE,
                SignupStep.REGION,
                SignupStep.AGE,
                SignupStep.GENDER,
                SignupStep.INTERESTS,
                SignupStep.BUDGET
        ));

        FLOW.put(UserType.INHABITANT, List.of(
                SignupStep.USER_TYPE,
                SignupStep.REGION,
                SignupStep.AGE,
                SignupStep.GENDER,
                SignupStep.INTERESTS,
                SignupStep.STARTUP
        ));
    }

    public static boolean isValidNextStep(UserType userType, SignupStep currentStep, SignupStep requestedStep) {
        List<SignupStep> steps = FLOW.get(userType);
        int currentIdx = steps.indexOf(currentStep);
        return currentIdx != -1
                && currentIdx + 1 < steps.size()
                && steps.get(currentIdx + 1) == requestedStep;
    }

    public static SignupStep nextStep(UserType userType, SignupStep currentStep) {
        List<SignupStep> steps = FLOW.get(userType);
        int currentIdx = steps.indexOf(currentStep);
        return (currentIdx != -1 && currentIdx + 1 < steps.size()) ? steps.get(currentIdx + 1) : null;
    }

    public static boolean isFirstStep(SignupStep step, UserType userType) {
        return FLOW.get(userType).get(0) == step;
    }

    public static boolean isLastStep(SignupStep step, UserType userType) {
        List<SignupStep> steps = FLOW.get(userType);
        return steps.get(steps.size() - 1) == step;
    }
}
