package com.example.soso.users.controller;

import com.example.soso.global.jwt.JwtTokenDto;
import com.example.soso.users.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증/인가 관련 API")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Access Token 재발급",
            description = "Refresh Token 쿠키가 유효하면 새로운 Access Token과 Refresh Token을 발급합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "토큰 재발급 성공",
                            content = @Content(schema = @Schema(implementation = JwtTokenDto.class))),
                    @ApiResponse(responseCode = "401", description = "Refresh Token이 유효하지 않음",
                            content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
            }
    )
    @PostMapping("/refresh")
    public ResponseEntity<JwtTokenDto> refreshToken(
            @Parameter(description = "HttpOnly 쿠키에 저장된 Refresh Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken == null) {
            return ResponseEntity.badRequest().build();
        }
        JwtTokenDto jwtToken = authService.refreshAccessToken(refreshToken, response);
        return ResponseEntity.ok(jwtToken);
    }

    @Operation(
            summary = "로그아웃",
            description = "Refresh Token을 무효화하고 쿠키를 삭제합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
                    @ApiResponse(responseCode = "400", description = "Refresh Token이 없음")
            }
    )
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Parameter(description = "HttpOnly 쿠키에 저장된 Refresh Token")
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken == null) {
            return ResponseEntity.badRequest().build();
        }
        authService.logout(refreshToken, response);
        return ResponseEntity.ok().build();
    }
}
