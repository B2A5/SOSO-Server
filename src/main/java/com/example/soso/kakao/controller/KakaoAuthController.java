package com.example.soso.kakao.controller;

import com.example.soso.kakao.dto.KakaoLoginRequest;
import com.example.soso.kakao.dto.KakaoLoginResponse;
import com.example.soso.kakao.service.KakaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증/인가 관련 API")
@RestController
@RequestMapping("/auth/kakao")
@RequiredArgsConstructor
public class KakaoAuthController {

    private final KakaoService kakaoService;

    @Operation(
            summary = "카카오 로그인",
            description = "카카오 인가 코드를 통해 사용자 로그인을 처리합니다. 기존 사용자는 JWT 토큰 및 사용자 정보를 반환하고, 신규 사용자는 회원가입 세션을 생성합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "로그인 성공",
                            content = @Content(
                                    schema = @Schema(implementation = KakaoLoginResponse.class),
                                    examples = {
                                            @ExampleObject(name = "기존 사용자", value = "{\"isNewUser\": false, \"accessToken\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\", \"user\": {\"id\": \"550e8400-e29b-41d4-a716-446655440000\", \"username\": \"홍길동\", \"nickname\": \"길동이\", \"email\": \"user@example.com\"}}"),
                                            @ExampleObject(name = "신규 사용자", value = "{\"isNewUser\": true, \"accessToken\": null, \"user\": null}")
                                    }
                            )),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청 형식"),
                    @ApiResponse(responseCode = "500", description = "서버 오류")
            }
    )
    @PostMapping("/login")
    public ResponseEntity<KakaoLoginResponse> kakaoLogin(
            @RequestBody KakaoLoginRequest request,
            HttpSession session,
            HttpServletResponse response
    ) {
        KakaoLoginResponse result = kakaoService.login(
                request.code(),
                request.codeVerifier(),
                request.redirectUri(),
                session,
                response
        );
        return ResponseEntity.ok(result);
    }
}
