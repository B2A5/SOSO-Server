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
    UNAUTHORIZED_ACCESS("접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    EMAIL_ALREADY_REGISTERED("이미 등록된 이메일 입니다.", HttpStatus.BAD_REQUEST),
    STEPS_NOT_TYPE("현재 진행 단계에서 허용되지 않는 요청입니다.", HttpStatus.BAD_REQUEST),
    SESSION_NOT_VALID("회원가입 세션이 만료되었습니다. 다시 로그인 해주세요.", HttpStatus.UNAUTHORIZED);

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
