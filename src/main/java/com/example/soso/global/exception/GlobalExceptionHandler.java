package com.example.soso.global.exception;

import com.example.soso.global.exception.domain.BaseErrorCode;
import com.example.soso.global.exception.domain.ErrorResponse;
import com.example.soso.global.exception.util.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 커스텀 BaseException 처리
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex) {
        BaseErrorCode errorCode = ex.getErrorCode();

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(new ErrorResponse(errorCode.name(), errorCode.getMessage()));
    }

    /**
     * @Valid 실패 시 발생하는 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();

        String message;
        if (fieldError != null) {
            message = "[" + fieldError.getField() + "] " + fieldError.getDefaultMessage();
        } else {
            message = "잘못된 요청입니다.";
        }

        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse("VALIDATION_ERROR", message));
    }

    /**
     * 예상치 못한 모든 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex) {
        log.error("예상치 못한 예외가 발생했습니다.", ex);

        return ResponseEntity
                .status(500)
                .body(new ErrorResponse("INTERNAL_SERVER_ERROR", "예상치 못한 오류가 발생했습니다."));
    }
}
