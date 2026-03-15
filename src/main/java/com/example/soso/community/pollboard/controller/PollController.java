package com.example.soso.community.pollboard.controller;

import com.example.soso.community.pollboard.domain.dto.*;
import com.example.soso.community.pollboard.domain.entity.PollStatus;
import com.example.soso.community.pollboard.service.PollService;
import com.example.soso.global.exception.domain.ErrorResponse;
import com.example.soso.security.domain.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 투표 게시판 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/community/polls")
@RequiredArgsConstructor
@Tag(name = "Poll", description = "투표 게시판 API")
public class PollController {

    private final PollService pollService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            operationId = "createPoll",
            summary = "투표 게시글 작성",
            description = """
                    새로운 투표 게시글을 작성합니다.

                    **특징:**
                    - 2-5개의 투표 옵션 필수
                    - 이미지 업로드 지원 (최대 4장)
                    - 투표 마감 시간 설정 필수
                    - 재투표 허용 여부 설정
                    - 중복 선택 허용 여부 설정
                    - Multipart 방식으로 이미지와 텍스트 데이터를 함께 전송
                    - 카테고리 필수 선택

                    **지원 파일 형식:** jpg, jpeg, png, gif, webp
                    **최대 파일 크기:** 5MB per image
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = PollCreateRequest.class)
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "투표 게시글 작성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PollCreateResponse.class),
                            examples = @ExampleObject(
                                    name = "성공 응답",
                                    value = "{\"postId\": 1}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (검증 실패, 이미지 관련 오류)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "빈 제목", value = "{\"code\": \"VALIDATION_FAILED\", \"message\": \"[title] 제목은 필수입니다.\"}"),
                                    @ExampleObject(name = "빈 내용", value = "{\"code\": \"VALIDATION_FAILED\", \"message\": \"[content] 내용은 필수입니다.\"}"),
                                    @ExampleObject(name = "옵션 개수 부족", value = "{\"code\": \"BAD_REQUEST\", \"message\": \"투표 옵션은 최소 2개, 최대 5개까지 가능합니다.\"}"),
                                    @ExampleObject(name = "과거 마감 시간", value = "{\"code\": \"BAD_REQUEST\", \"message\": \"투표 마감 시간은 미래 시간이어야 합니다.\"}"),
                                    @ExampleObject(name = "이미지 개수 초과", value = "{\"code\": \"ILLEGAL_ARGUMENT\", \"message\": \"이미지는 최대 4장까지 업로드 가능합니다.\"}")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "인증 실패",
                                    value = "{\"code\": \"AUTHENTICATION_FAILED\", \"message\": \"인증이 필요합니다.\"}"
                            )
                    )
            )
    })
    public ResponseEntity<PollCreateResponse> createPoll(
            @ModelAttribute @Valid PollCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long pollId = pollService.createPoll(request, userDetails.getUser().getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new PollCreateResponse(pollId));
    }

    @GetMapping("/{pollId}")
    @Operation(
            operationId = "getPoll",
            summary = "투표 게시글 상세 조회",
            description = """
                    투표 게시글의 상세 정보를 조회합니다.

                    **특징:**
                    - 조회 시 조회수 자동 증가
                    - 인증/비인증 사용자 모두 조회 가능
                    - 인증 여부에 따라 hasVoted, isLiked, canEdit, canDelete 값 변경
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "투표 게시글 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PollDetailResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시글을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"POST_NOT_FOUND\", \"message\": \"해당 게시글을 찾을 수 없습니다.\"}")
                    )
            )
    })
    public ResponseEntity<PollDetailResponse> getPoll(
            @Parameter(description = "투표 게시글 ID", required = true)
            @PathVariable Long pollId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        String userId = null;
        if (userDetails != null) {
            userId = userDetails.getUser().getId();
        }

        PollDetailResponse response = pollService.getPoll(pollId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(
            operationId = "getPollsByCursor",
            summary = "투표 게시글 목록 조회 (커서 기반)",
            description = """
                    커서 기반 페이지네이션으로 투표 게시글 목록을 조회합니다.

                    **정렬 옵션:**
                    - LATEST: 최신순 (기본값)
                    - LIKE: 투표순 (투표 인원 많은 순)
                    - COMMENT: 댓글순
                    - VIEW: 조회순
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "투표 게시글 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PollCursorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 파라미터",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "잘못된 정렬 값", value = "{\"code\": \"INVALID_ENUM_VALUE\", \"message\": \"'INVALID_SORT'은(는) 허용되지 않는 값입니다. 사용 가능한 값: [LATEST, LIKE, COMMENT, VIEW]\"}"),
                                    @ExampleObject(name = "잘못된 커서", value = "{\"code\": \"INVALID_CURSOR\", \"message\": \"유효하지 않은 커서 값입니다.\"}")
                            }
                    )
            )
    })
    public ResponseEntity<PollCursorResponse> getPollList(
            @Parameter(description = "투표 상태 (IN_PROGRESS: 진행중, COMPLETED: 완료, null: 전체)")
            @RequestParam(required = false) PollStatus status,
            @Parameter(
                    description = """
                            정렬 기준

                            - LATEST: 최신순 (기본값)
                            - LIKE: 투표순 (투표 인원 많은 순)
                            - COMMENT: 댓글순
                            - VIEW: 조회순
                            """,
                    example = "LATEST"
            )
            @RequestParam(defaultValue = "LATEST") com.example.soso.community.pollboard.dto.PollSortType sort,
            @Parameter(description = "페이지 크기 (1-50, 기본값: 20)", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(
                    description = """
                            커서 기반 페이징을 위한 커서 값

                            **첫 요청:** cursor 없이 요청
                            **다음 페이지:** 이전 응답의 nextCursor 값 사용
                            """,
                    example = "42"
            )
            @RequestParam(required = false) String cursor,
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        String userId = null;
        if (userDetails != null) {
            userId = userDetails.getUser().getId();
        }
        PollCursorResponse response = pollService.getPollsByCursor(status, sort, size, cursor, userId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping(value = "/{pollId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            operationId = "updatePoll",
            summary = "투표 게시글 수정",
            description = """
                    투표 게시글을 수정합니다. 투표 옵션은 수정할 수 없습니다.

                    **수정 가능 항목:**
                    - 카테고리
                    - 제목, 내용
                    - 이미지 (추가/삭제)
                    - 투표 설정 (투표 시작 전에만 가능)

                    **Multipart 업로드**
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = PollUpdateRequest.class)
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "투표 게시글 수정 성공"),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (작성자가 아님)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"UNAUTHORIZED_ACCESS\", \"message\": \"접근 권한이 없습니다.\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시글을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"POST_NOT_FOUND\", \"message\": \"해당 게시글을 찾을 수 없습니다.\"}")
                    )
            )
    })
    public ResponseEntity<Void> updatePoll(
            @Parameter(description = "투표 게시글 ID", required = true)
            @PathVariable Long pollId,
            @ModelAttribute @Valid PollUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        pollService.updatePoll(pollId, request, userDetails.getUser().getId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{pollId}")
    @Operation(
            operationId = "deletePoll",
            summary = "투표 게시글 삭제",
            description = "투표 게시글을 삭제합니다 (소프트 삭제)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "투표 게시글 삭제 성공"),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (작성자가 아님)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"UNAUTHORIZED_ACCESS\", \"message\": \"접근 권한이 없습니다.\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시글을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"POST_NOT_FOUND\", \"message\": \"해당 게시글을 찾을 수 없습니다.\"}")
                    )
            )
    })
    public ResponseEntity<Void> deletePoll(
            @Parameter(description = "투표 게시글 ID", required = true)
            @PathVariable Long pollId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        pollService.deletePoll(pollId, userDetails.getUser().getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{pollId}/vote")
    @Operation(
            operationId = "castVote",
            summary = "투표 참여",
            description = """
                    투표에 참여합니다.

                    **제약사항:**
                    - 한 투표당 1번만 참여 가능 (중복 투표 불가)
                    - 진행 중인 투표에만 참여 가능
                    - 선택한 옵션은 해당 투표의 옵션이어야 함

                    **권한:** 로그인 사용자만 가능
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "투표 참여 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(name = "투표 마감", value = "{\"code\": \"VOTE_CLOSED\", \"message\": \"투표가 마감되었습니다.\"}"),
                                    @ExampleObject(name = "잘못된 옵션", value = "{\"code\": \"INVALID_VOTE_OPTION\", \"message\": \"유효하지 않은 투표 옵션입니다.\"}")
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "이미 투표에 참여함",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "중복 투표", value = "{\"code\": \"ALREADY_VOTED\", \"message\": \"이미 투표에 참여하였습니다.\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시글 또는 옵션을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "게시글 없음", value = "{\"code\": \"POST_NOT_FOUND\", \"message\": \"해당 게시글을 찾을 수 없습니다.\"}")
                    )
            )
    })
    public ResponseEntity<Void> vote(
            @Parameter(description = "투표 게시글 ID", required = true, example = "1")
            @PathVariable Long pollId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "투표 옵션 선택 정보",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = VoteRequest.class),
                            examples = {
                                    @ExampleObject(name = "단일 선택", value = "{\"voteOptionIds\": [1]}", description = "allowMultipleChoice = false인 경우"),
                                    @ExampleObject(name = "중복 선택", value = "{\"voteOptionIds\": [1, 2, 3]}", description = "allowMultipleChoice = true인 경우")
                            }
                    )
            )
            @Valid @RequestBody VoteRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        pollService.vote(pollId, request, userDetails.getUser().getId());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{pollId}/vote")
    @Operation(
            operationId = "changeVote",
            summary = "투표 변경 (재투표)",
            description = """
                    이미 참여한 투표를 변경합니다.

                    **제약사항:**
                    - 재투표가 허용된 경우에만 가능 (allowRevote = true)
                    - 진행 중인 투표에만 가능
                    - 기존에 투표한 기록이 있어야 함

                    **권한:** 로그인 사용자만 가능
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "투표 변경 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "투표 마감",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "투표 마감", value = "{\"code\": \"VOTE_CLOSED\", \"message\": \"투표가 마감되었습니다.\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "재투표 허용되지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "재투표 불가", value = "{\"code\": \"REVOTE_NOT_ALLOWED\", \"message\": \"재투표가 허용되지 않습니다.\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "투표 기록을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(name = "투표 기록 없음", value = "{\"code\": \"VOTE_NOT_FOUND\", \"message\": \"투표 기록을 찾을 수 없습니다.\"}")
                    )
            )
    })
    public ResponseEntity<Void> changeVote(
            @Parameter(description = "투표 게시글 ID", required = true, example = "1")
            @PathVariable Long pollId,
            @Valid @RequestBody VoteRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        pollService.changeVote(pollId, request, userDetails.getUser().getId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{pollId}/vote")
    @Operation(
            operationId = "cancelVote",
            summary = "투표 취소",
            description = "참여한 투표를 취소합니다. 재투표가 허용된 경우에만 가능합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "투표 취소 성공"),
            @ApiResponse(
                    responseCode = "403",
                    description = "재투표 허용되지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"REVOTE_NOT_ALLOWED\", \"message\": \"재투표가 허용되지 않습니다.\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "투표 기록을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(value = "{\"code\": \"VOTE_NOT_FOUND\", \"message\": \"투표 기록을 찾을 수 없습니다.\"}")
                    )
            )
    })
    public ResponseEntity<Void> cancelVote(
            @Parameter(description = "투표 게시글 ID", required = true)
            @PathVariable Long pollId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        pollService.cancelVote(pollId, userDetails.getUser().getId());
        return ResponseEntity.ok().build();
    }
}
