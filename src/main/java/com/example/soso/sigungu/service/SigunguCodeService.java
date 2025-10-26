package com.example.soso.sigungu.service;

import com.example.soso.sigungu.domain.entity.SigunguCode;
import com.example.soso.sigungu.repository.SigunguCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 시군구 코드 조회 서비스
 *
 * 시군구 코드(5자리)를 도시명으로 변환하는 기능 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SigunguCodeService {

    private final SigunguCodeRepository sigunguCodeRepository;

    /**
     * 기본 대체 지역명
     * 시군구 코드를 찾을 수 없거나 유효하지 않은 경우 반환
     */
    private static final String DEFAULT_LOCATION = "소소 타운";

    /**
     * 시군구 코드를 도시명으로 변환
     *
     * @param sigunguCode 5자리 시군구 코드 (예: "11110")
     * @return 도시명 (예: "서울특별시 종로구") 또는 변환 실패 시 "소소 타운"
     */
    public String convertToAddress(String sigunguCode) {
        // null 또는 빈 문자열 체크
        if (sigunguCode == null || sigunguCode.isBlank()) {
            log.debug("시군구 코드가 null 또는 빈 문자열입니다. 기본값 반환: {}", DEFAULT_LOCATION);
            return DEFAULT_LOCATION;
        }

        // 5자리 숫자 형식 검증
        if (!SigunguCode.isValidCode(sigunguCode)) {
            log.warn("유효하지 않은 시군구 코드 형식: {}. 기본값 반환: {}", sigunguCode, DEFAULT_LOCATION);
            return DEFAULT_LOCATION;
        }

        // 데이터베이스에서 조회
        return sigunguCodeRepository.findByCode(sigunguCode)
                .map(SigunguCode::getFullName)
                .orElseGet(() -> {
                    log.warn("시군구 코드 {}에 해당하는 지역을 찾을 수 없습니다. 기본값 반환: {}", sigunguCode, DEFAULT_LOCATION);
                    return DEFAULT_LOCATION;
                });
    }

    /**
     * 여러 시군구 코드를 안전하게 변환
     * 각 코드에 대해 독립적으로 변환을 시도하며, 실패 시 기본값 반환
     *
     * @param sigunguCode 시군구 코드
     * @return 변환된 주소 또는 기본값
     */
    public String convertToAddressSafe(String sigunguCode) {
        try {
            return convertToAddress(sigunguCode);
        } catch (Exception e) {
            log.error("시군구 코드 변환 중 예외 발생. code: {}, error: {}", sigunguCode, e.getMessage(), e);
            return DEFAULT_LOCATION;
        }
    }

    /**
     * 기본 대체 지역명 반환
     *
     * @return "소소 타운"
     */
    public String getDefaultLocation() {
        return DEFAULT_LOCATION;
    }
}
