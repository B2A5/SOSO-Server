package com.example.soso.users.domain.dto;

import com.example.soso.community.common.post.domain.dto.UserSummaryResponse;
import com.example.soso.users.domain.entity.Users;

import java.util.stream.Collectors;

public class UserMapper {

    public static Users fromSignupSession(SignupSession session, String username, String email, String profileImageUrl) {
        return Users.builder()
                .username(username)
                .nickname(session.getNickname())
                .email(email)
                .userType(session.getUserType())
                .gender(session.getGender())
                .ageRange(session.getAgeRange())
                .budget(session.getBudget())
                .startupExperience(session.getStartupExperience())
                .location(session.getRegionId())
                .interests(session.getInterests())
                .profileImageUrl(profileImageUrl)
                .build();
    }

    public static UserSummaryResponse toUserSummary(Users users){
        return new UserSummaryResponse(users.getId(), users.getUsername(), users.getLocation(), users.getProfileImageUrl(), users.getUserType());
    }

    public static UserResponse toUserResponse(Users user) {
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
                .location(user.getLocation())
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
