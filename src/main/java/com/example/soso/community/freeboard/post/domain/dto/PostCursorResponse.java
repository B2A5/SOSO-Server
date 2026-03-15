package com.example.soso.community.freeboard.post.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "게시글 커서 기반 조회 응답")
public record PostCursorResponse(

        @Schema(description = "게시글 목록", requiredMode = Schema.RequiredMode.REQUIRED)
        List<PostSummaryResponse> posts,

        @Schema(description = "다음 페이지 조회를 위한 커서 정보", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        CursorDto nextCursor

) {}
