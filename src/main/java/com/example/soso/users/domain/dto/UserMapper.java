package com.example.soso.users.domain.dto;

import com.example.soso.community.common.post.domain.dto.UserSummaryResponse;
import com.example.soso.users.domain.entity.Users;

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
        return new UserSummaryResponse(users.getUsername(), users.getLocation(), users.getProfileImageUrl(), users.getUserType());
    }

}
