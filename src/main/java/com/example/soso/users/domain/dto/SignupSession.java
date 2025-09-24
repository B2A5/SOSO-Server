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

/**
 * 회원가입 진행 상황을 세션에 저장하기 위한 DTO.
 * 프런트/백 간의 상태 공유를 위해 필요한 최소 정보만을 담는다.
 *  - currentStep : 사용자가 현재 어디까지 완료했는지
 *  - userType 등 : 단계 전환에 필요한 누적 입력값
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SignupSession implements Serializable {

    private String username;
    private String email;
    private String profileImageUrl;
    private UserType userType;        // 예비창업자 or 주민
    private String regionId;          // 지역 코드 또는 ID
    private AgeRange ageRange;        // 10대 ~ 60대 이상
    private Gender gender;            // 남성 / 여성
    private List<InterestType> interests;   // 관심 업종 (문자열 목록)
    private BudgetRange budget;            // 예산 (null 가능 → 건너뛰기)
    private SignupStep currentStep;
    private StartupExperience startupExperience;
    private String nickname;
}
