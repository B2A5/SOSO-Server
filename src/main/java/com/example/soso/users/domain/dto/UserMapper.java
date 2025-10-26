package com.example.soso.users.domain.dto;

import com.example.soso.community.common.post.domain.dto.UserSummaryResponse;
import com.example.soso.sigungu.service.SigunguCodeService;
import com.example.soso.users.domain.entity.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * User 엔티티와 DTO 간 변환을 담당하는 매퍼
 *
 * 시군구 코드를 도시명으로 변환하는 기능 포함
 */
@Component
@RequiredArgsConstructor
public class UserMapper {

    private final SigunguCodeService sigunguCodeService;

    public Users fromSignupSession(SignupSession session, String username, String email, String profileImageUrl) {
        return Users.builder()
                .username(username)
                .nickname(session.getNickname())
                .email(email)
                .userType(session.getUserType())
                .gender(session.getGender())
                .ageRange(session.getAgeRange())
                .budget(session.getBudget())
                .startupExperience(session.getStartupExperience())
                .location(session.getRegionId())  // 시군구 코드 저장 (5자리)
                .interests(session.getInterests())
                .profileImageUrl(profileImageUrl)
                .build();
    }

    /**
     * Users 엔티티를 UserSummaryResponse로 변환
     * location 필드는 시군구 코드 → 도시명으로 변환
     */
    public UserSummaryResponse toUserSummary(Users users) {
        String address = sigunguCodeService.convertToAddressSafe(users.getLocation());
        return new UserSummaryResponse(
                users.getId(),
                users.getUsername(),
                address,  // 시군구 코드를 도시명으로 변환
                users.getProfileImageUrl(),
                users.getUserType()
        );
    }

    /**
     * Users 엔티티를 UserResponse로 변환
     * location 필드는 시군구 코드 → 도시명으로 변환
     */
    public UserResponse toUserResponse(Users user) {
        String address = sigunguCodeService.convertToAddressSafe(user.getLocation());

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .userType(user.getUserType())
                .profileImageUrl(user.getProfileImageUrl())
                .gender(user.getGender())
                .ageRange(user.getAgeRange())
                .budget(user.getBudget() != null ? user.getBudget().getLabel() : null)
                .startupExperience(user.getStartupExperience() != null ? user.getStartupExperience().getLabel() : null)
                .location(address)  // 시군구 코드를 도시명으로 변환
                .interests(user.getInterests() != null
                        ? user.getInterests().stream()
                                .map(interest -> interest.getLabel())
                                .collect(Collectors.toList())
                        : null)
                .latitude(user.getLatitude())
                .longitude(user.getLongitude())
                .createdDate(user.getCreatedDate())
                .lastModifiedDate(user.getLastModifiedDate())
                .build();
    }

}
