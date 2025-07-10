package com.example.soso.kakao.service;

import com.example.soso.kakao.dto.KakaoUserProfileDto;
import com.example.soso.users.domain.dto.SignupSession;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

@Service
public class SignupSessionService {

    private static final String SIGNUP_SESSION_KEY = "signup";

    public void save(HttpSession session, KakaoUserProfileDto profile) {
        SignupSession signup = new SignupSession();
        signup.setEmail(profile.email());
        signup.setProfileImageUrl(profile.profileImageUrl());
        session.setAttribute(SIGNUP_SESSION_KEY, signup);
    }
}
