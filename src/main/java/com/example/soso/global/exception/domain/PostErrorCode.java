package com.example.soso.global.exception.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum PostErrorCode implements BaseErrorCode {

    POST_NOT_FOUND("해당 게시글을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    POST_ACCESS_DENIED("게시글에 대한 접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    NOT_FOUND("해당 게시글을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    FORBIDDEN("게시글에 대한 권한이 없습니다.", HttpStatus.FORBIDDEN),
    INVALID_CATEGORY("유효하지 않은 카테고리입니다.", HttpStatus.BAD_REQUEST),
    EMPTY_CONTENT("게시글 내용은 비어 있을 수 없습니다.", HttpStatus.BAD_REQUEST),
    LIKE_COUNT_NOT_FOUND("좋아요 수를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    COMMENT_NOT_FOUND("없는 댓글 입니다", HttpStatus.NOT_FOUND);

    private final String message;
    private final HttpStatus httpStatus;

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
