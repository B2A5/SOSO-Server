package com.example.soso.global.exception;

import com.example.soso.global.exception.domain.BaseErrorCode;
import com.example.soso.global.exception.domain.ErrorResponse;
import com.example.soso.global.exception.util.BaseException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import feign.FeignException;
import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 커스텀 BaseException 처리
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex) {
        BaseErrorCode errorCode = ex.getErrorCode();
        log.warn("Handled business exception: {} - {}", errorCode.name(), ex.getMessage());

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(new ErrorResponse(errorCode.name(), errorCode.getMessage()));
    }

    /**
     * JSON 파싱/역직렬화 실패 처리 (특히 Enum 값 오류)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        Throwable rootCause = ex.getMostSpecificCause();

        if (rootCause instanceof InvalidFormatException invalidFormatException
                && invalidFormatException.getTargetType().isEnum()) {
            Class<?> enumType = invalidFormatException.getTargetType();
            String invalidValue = String.valueOf(invalidFormatException.getValue());
            String allowedValues = Arrays.stream(enumType.getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));

            String message = String.format("'%s'은(는) 허용되지 않는 값입니다. 사용 가능한 값: [%s]",
                    invalidValue, allowedValues);

            log.warn("Invalid enum value received: enumType={}, invalidValue={}", enumType.getSimpleName(), invalidValue);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("INVALID_ENUM_VALUE", message));
        }

        log.warn("Failed to read HTTP message: {}", rootCause != null ? rootCause.getMessage() : ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_REQUEST_BODY", "요청 본문을 해석할 수 없습니다."));
    }

    /**
     * MethodArgumentTypeMismatchException 처리 (URL 파라미터 타입 오류, 특히 Enum 값 오류)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Class<?> targetType = ex.getRequiredType();

        if (targetType != null && targetType.isEnum()) {
            String invalidValue = String.valueOf(ex.getValue());
            String allowedValues = Arrays.stream(targetType.getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));

            String message = String.format("'%s'은(는) 허용되지 않는 값입니다. 사용 가능한 값: [%s]",
                    invalidValue, allowedValues);

            log.warn("Invalid enum parameter: parameterName={}, enumType={}, invalidValue={}",
                    ex.getName(), targetType.getSimpleName(), invalidValue);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("INVALID_ENUM_VALUE", message));
        }

        log.warn("Method argument type mismatch: parameterName={}, expectedType={}, actualValue={}",
                ex.getName(), targetType, ex.getValue());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_PARAMETER_TYPE", "잘못된 파라미터 타입입니다."));
    }

    /**
     * IllegalArgumentException -> 400으로 변환
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument encountered: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("ILLEGAL_ARGUMENT", ex.getMessage()));
    }

    /**
     * Feign 클라이언트 호출 실패 처리 (외부 API 호출 오류)
     */
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeignException(FeignException ex) {
        int status = ex.status();
        String responseBody = ex.contentUTF8();

        log.error("External API call failed: status={}, method={}, url={}, response={}",
                status, ex.request().httpMethod(), ex.request().url(), responseBody);

        // 외부 API의 4xx 에러는 클라이언트 에러로 처리
        if (status >= 400 && status < 500) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("EXTERNAL_API_CLIENT_ERROR",
                            "외부 API 호출이 실패했습니다. 요청 정보를 확인해주세요."));
        }

        // 외부 API의 5xx 에러는 서버 에러로 처리
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse("EXTERNAL_API_SERVER_ERROR",
                        "외부 서비스와의 통신 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."));
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
                .body(new ErrorResponse("VALIDATION_FAILED", message));
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
