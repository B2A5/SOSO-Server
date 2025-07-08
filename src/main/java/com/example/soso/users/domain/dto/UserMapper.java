package com.example.soso.users.domain.dto;

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
                .location(session.getRegionId())
                .interests(session.getInterests())
                .profileImageUrl(profileImageUrl)
                .build();
    }


}
