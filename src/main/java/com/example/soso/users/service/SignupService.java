package com.example.soso.users.service;

import com.example.soso.users.domain.entity.UserType;
import jakarta.servlet.http.HttpSession;

public interface SignupService {

    void saveUserType(HttpSession session, UserType userType);

    void saveRegion(HttpSession session, String regionId);
}
