package com.example.soso.users.service;

import com.example.soso.global.jwt.JwtTokenDto;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

    JwtTokenDto refreshAccessToken(String refreshToken, HttpServletResponse response);

}
