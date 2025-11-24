package com.example.soso.users.controller;

import com.example.soso.security.domain.CustomUserDetails;
import com.example.soso.users.domain.dto.UserResponse;
import com.example.soso.users.service.UsersService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Users", description = "사용자 정보 관련 API")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UsersController {

    private final UsersService usersService;

    @Operation(
            summary = "본인 정보 조회",
            description = """
                    현재 로그인한 사용자의 전체 정보를 조회합니다.

                    **인증 필요:** JWT Access Token이 필요합니다.

                    **응답 정보:**
                    - 기본 정보: ID, 사용자명, 닉네임, 이메일
                    - 프로필: 프로필 이미지 URL
                    - 개인 정보: 성별, 연령대
                    - 창업 정보: 사용자 유형, 예산, 창업 경험, 관심 업종
                    - 위치 정보: 지역명, 위도, 경도
                    - 시스템 정보: 생성일시, 수정일시
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "사용자 정보 조회 성공",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse"))
            )
    })
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        UserResponse userResponse = usersService.getCurrentUserInfo(userDetails.getUsername());
        return ResponseEntity.ok(userResponse);
    }

}
