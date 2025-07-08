package com.example.soso.users.domain.dto;

import com.example.soso.users.domain.entity.AgeRange;
import com.example.soso.users.domain.entity.BudgetRange;
import com.example.soso.users.domain.entity.Gender;
import com.example.soso.users.domain.entity.InterestType;
import com.example.soso.users.domain.entity.SignupStep;
import com.example.soso.users.domain.entity.StartupExperience;
import com.example.soso.users.domain.entity.UserType;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SignupSession implements Serializable {

    private UserType userType;        // 예비창업자 or 주민
    private String regionId;          // 지역 코드 또는 ID
    private AgeRange ageRange;        // 10대 ~ 60대 이상
    private Gender gender;            // 남성 / 여성
    private List<InterestType> interests;   // 관심 업종 (문자열 목록)
    private BudgetRange budget;            // 예산 (null 가능 → 건너뛰기)
    private SignupStep currentStep;
    private StartupExperience startupExperience;
}