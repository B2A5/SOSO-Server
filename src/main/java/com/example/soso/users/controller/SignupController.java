package com.example.soso.users.controller;

import com.example.soso.global.jwt.JwtTokenDto;
import com.example.soso.users.domain.dto.AgeRangeRequest;
import com.example.soso.users.domain.dto.BudgetRequest;
import com.example.soso.users.domain.dto.ExperienceRequest;
import com.example.soso.users.domain.dto.GenderRequest;
import com.example.soso.users.domain.dto.RegionRequest;
import com.example.soso.users.domain.dto.UserTypeRequest;
import com.example.soso.users.domain.entity.InterestRequest;
import com.example.soso.users.domain.entity.SignupStep;
import com.example.soso.users.service.SignupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "회원가입", description = """
**회원가입 단계별 API**

**전제조건**: 카카오 로그인 후 회원가입 세션이 생성되어 있어야 합니다.

**플로우**:
- **예비창업자(FOUNDER)**: user-type → region → age-range → gender → interests → budget → experience → nickname → complete
- **일반거주민(INHABITANT)**: user-type → region → age-range → gender → nickname → complete

각 단계는 순서대로 진행되어야 하며, 이전 단계가 완료되지 않으면 다음 단계로 진행할 수 없습니다.

**응답**: 모든 설정 API는 다음 단계를 반환합니다. (완료 API 제외)
""")
@RestController
@RequestMapping("/signup")
@RequiredArgsConstructor
public class SignupController {

    private final SignupService signupService;

    @Operation(
        summary = "[1단계] 사용자 유형 설정",
        description = "회원가입 첫 단계로, 예비창업자 또는 일반거주민을 선택합니다.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "사용자 유형 선택",
            content = @Content(
                schema = @Schema(implementation = UserTypeRequest.class),
                examples = {
                    @ExampleObject(name = "예비창업자", value = "{\"userType\": \"FOUNDER\"}"),
                    @ExampleObject(name = "일반거주민", value = "{\"userType\": \"INHABITANT\"}")
                }
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공 - 다음 단계 반환",
            content = @Content(schema = @Schema(implementation = SignupStep.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "세션이 유효하지 않음")
    })
    @PostMapping("/user-type")
    public ResponseEntity<SignupStep> setUserType(@RequestBody @Valid UserTypeRequest request,
                                                  @Parameter(hidden = true) HttpSession session) {
        SignupStep nextStep = signupService.saveUserType(session, request.userType());
        return ResponseEntity.ok(nextStep);
    }

    @Operation(
        summary = "[2단계] 지역 설정",
        description = "거주 지역을 설정합니다. 지역 코드는 행정구역 코드를 사용합니다.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "지역 코드",
            content = @Content(
                schema = @Schema(implementation = RegionRequest.class),
                examples = {
                    @ExampleObject(name = "종로구", value = "{\"regionId\": \"11110\"}"),
                    @ExampleObject(name = "강남구", value = "{\"regionId\": \"11680\"}"),
                    @ExampleObject(name = "마포구", value = "{\"regionId\": \"11560\"}")
                }
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공 - 다음 단계 반환"),
        @ApiResponse(responseCode = "400", description = "잘못된 지역 코드"),
        @ApiResponse(responseCode = "401", description = "세션이 유효하지 않음")
    })
    @PostMapping("/region")
    public ResponseEntity<SignupStep> setRegion(@RequestBody @Valid RegionRequest request,
                                                @Parameter(hidden = true) HttpSession session) {
        SignupStep nextStep = signupService.saveRegion(session, request.regionId());
        return ResponseEntity.ok(nextStep);
    }

    @Operation(
        summary = "[3단계] 연령대 설정",
        description = "사용자의 연령대를 설정합니다.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "연령대 선택",
            content = @Content(
                schema = @Schema(implementation = AgeRangeRequest.class),
                examples = {
                    @ExampleObject(name = "20대", value = "{\"ageRange\": \"TWENTIES\"}"),
                    @ExampleObject(name = "30대", value = "{\"ageRange\": \"THIRTIES\"}"),
                    @ExampleObject(name = "40대", value = "{\"ageRange\": \"FORTIES\"}")
                }
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공 - 다음 단계 반환"),
        @ApiResponse(responseCode = "400", description = "잘못된 연령대"),
        @ApiResponse(responseCode = "401", description = "세션이 유효하지 않음")
    })
    @PostMapping("/age-range")
    public ResponseEntity<SignupStep> setAgeRange(@RequestBody @Valid AgeRangeRequest request,
                                                  @Parameter(hidden = true) HttpSession session) {
        SignupStep nextStep = signupService.saveAgeRange(session, request.ageRange());
        return ResponseEntity.ok(nextStep);
    }

    @Operation(
        summary = "[4단계] 성별 설정",
        description = "사용자의 성별을 설정합니다.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "성별 선택",
            content = @Content(
                schema = @Schema(implementation = GenderRequest.class),
                examples = {
                    @ExampleObject(name = "남성", value = "{\"gender\": \"MALE\"}"),
                    @ExampleObject(name = "여성", value = "{\"gender\": \"FEMALE\"}")
                }
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공 - 다음 단계 반환"),
        @ApiResponse(responseCode = "400", description = "잘못된 성별"),
        @ApiResponse(responseCode = "401", description = "세션이 유효하지 않음")
    })
    @PostMapping("/gender")
    public ResponseEntity<SignupStep> setGender(@RequestBody @Valid GenderRequest request,
                                                @Parameter(hidden = true) HttpSession session) {
        SignupStep nextStep = signupService.saveGender(session, request.gender());
        return ResponseEntity.ok(nextStep);
    }

    @Operation(
        summary = "[5단계] 관심업종 설정 (예비창업자 전용)",
        description = "예비창업자가 관심있는 업종을 설정합니다. 여러 개 선택 가능하며, 빈 배열도 허용됩니다.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "관심 업종 목록",
            content = @Content(
                schema = @Schema(implementation = InterestRequest.class),
                examples = {
                    @ExampleObject(name = "단일 선택", value = "{\"interests\": [\"ACCOMMODATION_FOOD\"]}"),
                    @ExampleObject(name = "복수 선택", value = "{\"interests\": [\"MANUFACTURING\", \"WHOLESALE_RETAIL\", \"ACCOMMODATION_FOOD\"]}"),
                    @ExampleObject(name = "선택 안함", value = "{\"interests\": []}")
                }
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공 - 다음 단계 반환"),
        @ApiResponse(responseCode = "400", description = "잘못된 업종 코드 또는 일반거주민 접근"),
        @ApiResponse(responseCode = "401", description = "세션이 유효하지 않음")
    })
    @PostMapping("/interests")
    public ResponseEntity<SignupStep> setInterests(@RequestBody @Valid InterestRequest request,
                                                   @Parameter(hidden = true) HttpSession session) {
        SignupStep nextStep = signupService.saveInterests(session, request.interests());
        return ResponseEntity.ok(nextStep);
    }

    @Operation(
        summary = "[6단계] 예산 설정 (예비창업자 전용)",
        description = "예비창업자의 창업 예산을 설정합니다. null 값 허용 (건너뛰기 가능).",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "예산 구간 (선택사항)",
            content = @Content(
                schema = @Schema(implementation = BudgetRequest.class),
                examples = {
                    @ExampleObject(name = "3천~5천만원", value = "{\"budget\": \"THOUSANDS_3000_5000\"}"),
                    @ExampleObject(name = "1억 이상", value = "{\"budget\": \"OVER_1B\"}"),
                    @ExampleObject(name = "건너뛰기", value = "{\"budget\": null}")
                }
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공 - 다음 단계 반환"),
        @ApiResponse(responseCode = "400", description = "잘못된 예산 구간 또는 일반거주민 접근"),
        @ApiResponse(responseCode = "401", description = "세션이 유효하지 않음")
    })
    @PostMapping("/budget")
    public ResponseEntity<SignupStep> setBudget(@RequestBody @Valid BudgetRequest request,
                                                @Parameter(hidden = true) HttpSession session) {
        SignupStep nextStep = signupService.saveBudget(session, request.budget());
        return ResponseEntity.ok(nextStep);
    }

    @Operation(
        summary = "[7단계] 창업 경험 설정 (예비창업자 전용)",
        description = "예비창업자의 창업 경험 여부를 설정합니다.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "창업 경험 여부",
            content = @Content(
                schema = @Schema(implementation = ExperienceRequest.class),
                examples = {
                    @ExampleObject(name = "경험 있음", value = "{\"experience\": \"YES\"}"),
                    @ExampleObject(name = "경험 없음", value = "{\"experience\": \"NO\"}")
                }
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "성공 - 다음 단계 반환"),
        @ApiResponse(responseCode = "400", description = "잘못된 경험 값 또는 일반거주민 접근"),
        @ApiResponse(responseCode = "401", description = "세션이 유효하지 않음")
    })
    @PostMapping("/experience")
    public ResponseEntity<SignupStep> setExperience(@RequestBody @Valid ExperienceRequest request,
                                                    @Parameter(hidden = true) HttpSession session) {
        SignupStep nextStep = signupService.saveExperience(session, request.experience());
        return ResponseEntity.ok(nextStep);
    }

    @Operation(
        summary = "[8단계] 닉네임 생성",
        description = "자동으로 고유한 닉네임을 생성합니다. 이미 사용중인 닉네임은 피해서 생성됩니다.",
        responses = {
            @ApiResponse(responseCode = "200", description = "성공 - 생성된 닉네임 반환",
                content = @Content(schema = @Schema(type = "string", example = "활발한코끼리123"))),
            @ApiResponse(responseCode = "401", description = "세션이 유효하지 않음")
        }
    )
    @PostMapping("/nickname")
    public ResponseEntity<String> saveNickname(@Parameter(hidden = true) HttpSession session) {
        String nickname = signupService.saveNiceName(session);
        return ResponseEntity.ok(nickname);
    }

    @Operation(
        summary = "[9단계] 회원가입 완료",
        description = "회원가입을 완료하고 사용자 계정을 생성합니다. JWT 토큰을 발급하고 Refresh Token은 HttpOnly 쿠키로 설정됩니다.",
        responses = {
            @ApiResponse(responseCode = "200", description = "성공 - Access Token 반환",
                content = @Content(
                    schema = @Schema(implementation = JwtTokenDto.class),
                    examples = @ExampleObject(value = "{\"accessToken\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\"}"))
                ),
            @ApiResponse(responseCode = "400", description = "회원가입 단계가 완료되지 않음"),
            @ApiResponse(responseCode = "401", description = "세션이 유효하지 않음")
        }
    )
    @PostMapping("/complete")
    public ResponseEntity<JwtTokenDto> completeSignup(@Parameter(hidden = true) HttpSession session,
                                                      @Parameter(hidden = true) HttpServletResponse response) {
        JwtTokenDto jwtAccessToken = signupService.completeSignup(session, response);
        return ResponseEntity.ok(jwtAccessToken);
    }

    @Operation(
        summary = "창업 경험 정보 조회 (예비창업자 전용)",
        description = "이전에 설정한 창업 경험 정보를 조회합니다. 뒤로가기 기능에서 사용됩니다.",
        responses = {
            @ApiResponse(responseCode = "200", description = "성공 - 창업 경험 정보 반환",
                content = @Content(
                    schema = @Schema(implementation = ExperienceRequest.class),
                    examples = @ExampleObject(value = "{\"experience\": \"YES\"}")
                )),
            @ApiResponse(responseCode = "400", description = "일반거주민 접근 또는 데이터 없음"),
            @ApiResponse(responseCode = "401", description = "세션이 유효하지 않음")
        }
    )
    @GetMapping("/experience/data")
    public ResponseEntity<ExperienceRequest> getExperience(@Parameter(hidden = true) HttpSession session) {
        return ResponseEntity.ok(signupService.getExperience(session));
    }
}
