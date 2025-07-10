package com.example.soso.users.controller;

import com.example.soso.jwt.JwtTokenDto;
import com.example.soso.users.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/refresh")
    public ResponseEntity<JwtTokenDto> refreshToken(@CookieValue("refreshToken") String refreshToken,
                                                    HttpServletResponse response) {
        JwtTokenDto jwtToken = authService.refreshAccessToken(refreshToken, response);
        return ResponseEntity.ok(jwtToken);
    }
}
