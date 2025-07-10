package com.example.soso.users.controller;

import com.example.soso.jwt.JwtTokenDto;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

    JwtTokenDto refreshAccessToken(String refreshToken, HttpServletResponse response);

}
