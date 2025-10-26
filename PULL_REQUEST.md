# feat: 이미지 업로드 에러 핸들링 개선 및 API 문서 업데이트

## 📋 Summary

이미지 업로드 시 발생하는 에러를 적절한 HTTP 상태 코드와 명확한 에러 메시지로 처리하도록 개선했습니다.

### 주요 변경사항

- ✅ Spring multipart 설정 추가 (파일 크기 제한 명시)
- ✅ 파일 크기 초과 시 413 에러 반환 (기존 500 에러 해결)
- ✅ 이미지 업로드 에러 시나리오 테스트 추가
- ✅ Swagger API 문서 개선 (모든 에러 케이스 문서화)

---

## 🔍 Problem

### 기존 문제점

1. **500 Internal Server Error 발생**
   - 5MB 이상의 큰 파일 업로드 시 의도하지 않은 500 에러 발생
   - Spring Boot의 기본 multipart 설정 미지정 (기본값 1MB)
   - ImageUploadService는 5MB까지 허용하지만 Spring이 먼저 차단

2. **에러 핸들링 누락**
   - `MaxUploadSizeExceededException` 처리 로직 없음
   - 적절한 HTTP 상태 코드 반환 실패

3. **테스트 커버리지 부족**
   - 이미지 업로드 관련 에러 시나리오 테스트 미존재

4. **API 문서 불충분**
   - Swagger에 이미지 업로드 관련 에러 응답 미기재

---

## 💡 Solution

### 1. Spring Multipart 설정 추가
**파일**: `application.yml`

```yaml
spring:
  servlet:
    multipart:
      enabled: true
      max-file-size: 5MB        # 개별 파일 최대 크기
      max-request-size: 25MB    # 전체 요청 크기 (이미지 4장 + 메타데이터)
```

- 애플리케이션 정책(5MB)과 Spring 설정 일치
- 총 요청 크기는 이미지 4장 + 폼 데이터 고려하여 25MB 설정

### 2. MaxUploadSizeExceededException 핸들러 추가
**파일**: `GlobalExceptionHandler.java`

```java
@ExceptionHandler(MaxUploadSizeExceededException.class)
public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
    long maxFileSize = ex.getMaxUploadSize();

    String message;
    if (maxFileSize > 0) {
        long maxFileSizeMB = maxFileSize / (1024 * 1024);
        message = String.format("파일 크기가 너무 큽니다. 최대 업로드 크기는 %dMB입니다.", maxFileSizeMB);
    } else {
        message = "파일 크기가 너무 큽니다. 업로드 크기 제한을 초과했습니다.";
    }

    log.warn("File upload size exceeded: maxSize={}, message={}", maxFileSize, ex.getMessage());

    return ResponseEntity
            .status(HttpStatus.PAYLOAD_TOO_LARGE)  // 413
            .body(new ErrorResponse("FILE_SIZE_EXCEEDED", message));
}
```

**개선 효과**:
- ✅ 500 에러 → 413 Payload Too Large로 변경
- ✅ 명확한 에러 코드 제공 (`FILE_SIZE_EXCEEDED`)
- ✅ 사용자 친화적인 에러 메시지

### 3. 포괄적인 테스트 추가
**파일**: `ErrorScenarioIntegrationTest.java`

새로운 테스트 메서드 `imageUploadErrorScenarios()` 추가:

```java
@Test
@DisplayName("📷 이미지 업로드 에러 시나리오: 파일 크기, 형식, 개수 제한")
void imageUploadErrorScenarios() throws Exception {
    // 1. 지원하지 않는 파일 형식 (text/plain)
    // 2. 파일 크기 초과 (6MB)
    // 3. 이미지 개수 초과 (5장)
    // 4. 빈 파일
    // 5. 정상 업로드 (4장)
}
```

**테스트 커버리지**:
- ✅ 파일 형식 검증 → 400 Bad Request
- ✅ 파일 크기 검증 → 413 Payload Too Large
- ✅ 파일 개수 검증 → 400 Bad Request
- ✅ 빈 파일 검증 → 400 Bad Request
- ✅ 정상 시나리오 → 200 OK

### 4. Swagger API 문서 개선
**파일**: `FreeboardController.java`

#### POST `/community/freeboard` 엔드포인트 문서화

**400 Bad Request 예시 추가**:
- 제목/내용 검증 실패
- 잘못된 카테고리 값
- **이미지 개수 초과** (최대 4장)
- **지원하지 않는 파일 형식** (jpeg, jpg, png, gif, webp만 허용)
- **빈 파일** 에러

**413 Payload Too Large 추가**:
```json
{
  "code": "FILE_SIZE_EXCEEDED",
  "message": "파일 크기가 너무 큽니다. 최대 업로드 크기는 5MB입니다."
}
```

#### PATCH `/community/freeboard/{freeboardId}` 엔드포인트 동일 적용

---

## 🧪 Test Results

### 빌드 및 테스트 성공
```bash
./gradlew clean build
```

**결과**: ✅ All 189 tests passed

### 주요 테스트 케이스
1. ✅ 지원하지 않는 파일 형식 업로드 → 400 + "지원하지 않는 파일 형식" 메시지
2. ✅ 6MB 파일 업로드 → 413 Payload Too Large
3. ✅ 5개 이미지 업로드 → 400 + 개수 초과 메시지
4. ✅ 빈 파일 업로드 → 400 + "파일이 비어있습니다" 메시지
5. ✅ 정상 4개 이미지 업로드 → 200 OK

---

## 📝 Error Handling Flow

### 파일 크기 초과 시
```
대용량 파일 업로드 (>5MB)
    ↓
Spring Multipart Filter에서 감지
    ↓
MaxUploadSizeExceededException 발생
    ↓
GlobalExceptionHandler 처리
    ↓
413 Payload Too Large 반환
{
  "code": "FILE_SIZE_EXCEEDED",
  "message": "파일 크기가 너무 큽니다. 최대 업로드 크기는 5MB입니다."
}
```

### 이미지 검증 실패 시
```
파일이 Spring 크기 체크 통과
    ↓
ImageUploadService.validateImages() 호출
    ↓
형식/개수/빈파일/크기 검증
    ↓
검증 실패 시 IllegalArgumentException 발생
    ↓
GlobalExceptionHandler 처리
    ↓
400 Bad Request 반환
{
  "code": "ILLEGAL_ARGUMENT",
  "message": "구체적인 에러 메시지"
}
```

---

## 📊 Changes Summary

### Modified Files
- `src/main/resources/application.yml` - Spring multipart 설정 추가
- `src/main/java/com/example/soso/global/exception/GlobalExceptionHandler.java` - 파일 크기 초과 핸들러 추가
- `src/main/java/com/example/soso/community/freeboard/post/controller/FreeboardController.java` - Swagger 문서 개선
- `src/test/java/com/example/soso/community/freeboard/integration/ErrorScenarioIntegrationTest.java` - 이미지 업로드 에러 테스트 추가

### Impact
- **사용자 경험 개선**: 명확한 에러 메시지로 문제 파악 용이
- **API 표준 준수**: 적절한 HTTP 상태 코드 사용 (413, 400)
- **문서화 완성도**: 모든 에러 케이스 Swagger에 명시
- **테스트 커버리지**: 이미지 업로드 관련 엣지 케이스 모두 테스트

---

## ✅ Test Plan

- [x] 지원하지 않는 파일 형식 업로드 테스트
- [x] 파일 크기 초과 업로드 테스트 (6MB)
- [x] 이미지 개수 초과 테스트 (5장)
- [x] 빈 파일 업로드 테스트
- [x] 정상 이미지 업로드 테스트 (4장)
- [x] 전체 테스트 스위트 실행 (189 tests)
- [x] Swagger 문서 검증
- [x] 빌드 성공 확인

---

## 🔗 Related Issues

이 PR은 이미지 업로드 시 500 에러가 발생하던 문제를 해결합니다.

---

🤖 Generated with [Claude Code](https://claude.com/claude-code)
