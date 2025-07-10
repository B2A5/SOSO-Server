package com.example.soso.global.exception.domain;

import com.example.soso.global.exception.domain.BaseErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum UserErrorCode implements BaseErrorCode {

    USER_NOT_FOUND("존재하지 않는 사용자입니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_USER_ID("이미 사용 중인 사용자 ID입니다.", HttpStatus.CONFLICT),
    INVALID_USER_ID("유효하지 않은 사용자 ID입니다.", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED_USER("인증되지 않은 사용자입니다.", HttpStatus.UNAUTHORIZED),
    EMAIL_ALREADY_REGISTERED("이미 등록된 이메일 입니다.", HttpStatus.BAD_REQUEST),
    STEPS_NOT_TYPE("현재 사용자 맞지 않는 단계 입니다.", HttpStatus.BAD_REQUEST),
    SESSION_NOT_VALID("유효 하지 않은 세션 입니다.", HttpStatus.UNAUTHORIZED);

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
