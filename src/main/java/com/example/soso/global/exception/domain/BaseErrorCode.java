package com.example.soso.global.exception.domain;

import org.springframework.http.HttpStatus;

public interface BaseErrorCode {
    String name();                     // 에러 코드 (e.g., INVALID_TOKEN)
    String getMessage();              // 사용자에게 보여줄 메시지
    HttpStatus getHttpStatus();       // 적절한 HTTP 상태코드
}
