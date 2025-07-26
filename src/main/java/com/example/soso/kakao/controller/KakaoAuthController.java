package com.example.soso.kakao.controller;

import com.example.soso.kakao.dto.KakaoLoginRequest;
import com.example.soso.kakao.dto.KakaoLoginResult;
import com.example.soso.kakao.service.KakaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Kakao Auth", description = "카카오 소셜 로그인 API")
@RestController
@RequestMapping("/auth/kakao")
@RequiredArgsConstructor
public class KakaoAuthController {

    private final KakaoService kakaoService;

    @Operation(
            summary = "카카오 로그인",
            description = "카카오 인가 코드를 통해 사용자 로그인을 처리하고 JWT 토큰을 반환합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "로그인 성공",
                            content = @Content(schema = @Schema(implementation = KakaoLoginResult.class))),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청 형식"),
                    @ApiResponse(responseCode = "500", description = "서버 오류")
            }
    )
    @PostMapping("/login")
    public ResponseEntity<KakaoLoginResult> kakaoLogin(
            @RequestBody KakaoLoginRequest request,
            HttpSession session,
            HttpServletResponse response
    ) {
        KakaoLoginResult result = kakaoService.login(
                request.code(),
                request.codeVerifier(),
                session,
                response
        );
        return ResponseEntity.ok(result);
    }
}
