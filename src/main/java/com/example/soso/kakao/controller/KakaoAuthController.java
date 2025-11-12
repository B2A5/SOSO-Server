package com.example.soso.kakao.controller;

import com.example.soso.global.exception.domain.ErrorResponse;
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
            description = """
                    카카오 인가 코드를 통해 사용자 로그인을 처리합니다.

                    **토큰 발급 방식:**
                    - Response Body: accessToken 포함 (모바일 앱 지원)
                    - Set-Cookie 헤더: accessToken, refreshToken 쿠키 설정 (웹 브라우저 자동 관리)

                    **쿠키 보안 속성:**
                    - accessToken: HttpOnly=true, Secure=true, SameSite=None (XSS 방어, 30분)
                    - refreshToken: HttpOnly=true, Secure=true, SameSite=None (XSS 방어, 14일)

                    **클라이언트별 사용 방법:**
                    - 웹 브라우저: 쿠키 자동 관리, credentials: 'include' 설정 필요
                    - 모바일 앱: Body에서 accessToken 추출 후 AsyncStorage/SharedPreferences 저장

                    **기존 사용자:** JWT 토큰 및 사용자 정보 반환
                    **신규 사용자:** 회원가입 세션 생성
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "로그인 성공",
                            content = @Content(
                                    schema = @Schema(implementation = KakaoLoginResponse.class),
                                    examples = {
                                            @ExampleObject(name = "기존 사용자",
                                                    description = "기존 사용자 로그인 - 토큰 및 사용자 정보 반환",
                                                    value = "{\"isNewUser\": false, \"accessToken\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\", \"user\": {\"id\": \"550e8400-e29b-41d4-a716-446655440000\", \"username\": \"홍길동\", \"nickname\": \"길동이\", \"email\": \"user@example.com\", \"userType\": \"FOUNDER\", \"gender\": \"MALE\", \"ageRange\": \"TWENTIES\", \"location\": \"서울시 강남구\", \"createdDate\": \"2024-01-01T00:00:00\", \"lastModifiedDate\": \"2024-01-01T00:00:00\"}}"),
                                            @ExampleObject(name = "신규 사용자",
                                                    description = "신규 사용자 - 회원가입 세션 생성됨",
                                                    value = "{\"isNewUser\": true, \"accessToken\": null, \"user\": null}")
                                    }
                            )),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청 형식 또는 유효하지 않은 인가 코드",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @ExampleObject(name = "유효하지 않은 요청",
                                                    description = "필수 파라미터 누락 또는 형식 오류",
                                                    value = "{\"code\": \"VALIDATION_FAILED\", \"message\": \"[code] 값을 입력해주세요.\"}"),
                                            @ExampleObject(name = "카카오 인증 실패",
                                                    description = "카카오 OAuth 인증 실패",
                                                    value = "{\"code\": \"EXTERNAL_API_CLIENT_ERROR\", \"message\": \"외부 API 호출이 실패했습니다. 요청 정보를 확인해주세요.\"}")
                                    }
                            )),
                    @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "서버 오류",
                                            description = "예상치 못한 서버 오류",
                                            value = "{\"code\": \"INTERNAL_SERVER_ERROR\", \"message\": \"예상치 못한 오류가 발생했습니다.\"}"
                                    )
                            )),
                    @ApiResponse(responseCode = "502", description = "외부 API 통신 오류",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "외부 서비스 오류",
                                            description = "카카오 서버 오류",
                                            value = "{\"code\": \"EXTERNAL_API_SERVER_ERROR\", \"message\": \"외부 서비스와의 통신 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.\"}"
                                    )
                            ))
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
