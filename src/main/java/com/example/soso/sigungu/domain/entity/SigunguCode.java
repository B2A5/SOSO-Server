package com.example.soso.sigungu.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 시군구 코드 엔티티
 *
 * 행정구역코드(5자리)를 기반으로 도시명을 조회하기 위한 엔티티
 * 예: 11110 -> "서울특별시 종로구"
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "sigungu_code", indexes = {
    @Index(name = "idx_sigungu_code", columnList = "code")
})
public class SigunguCode {

    /**
     * 시군구 코드 (5자리)
     * 예: 11110 (서울특별시 종로구)
     */
    @Id
    @Column(name = "code", length = 5, nullable = false)
    private String code;

    /**
     * 시도명 (광역시/도)
     * 예: "서울특별시"
     */
    @Column(name = "sido", length = 50, nullable = false)
    private String sido;

    /**
     * 시군구명
     * 예: "종로구"
     */
    @Column(name = "sigungu", length = 50, nullable = false)
    private String sigungu;

    /**
     * 전체 지역명 (시도 + 시군구)
     * 예: "서울특별시 종로구"
     */
    @Column(name = "full_name", length = 100, nullable = false)
    private String fullName;

    @Builder
    public SigunguCode(String code, String sido, String sigungu, String fullName) {
        this.code = code;
        this.sido = sido;
        this.sigungu = sigungu;
        this.fullName = fullName;
    }

    /**
     * 시군구 코드가 유효한지 검증
     * 5자리 숫자여야 함
     */
    public static boolean isValidCode(String code) {
        if (code == null || code.length() != 5) {
            return false;
        }
        return code.matches("\\d{5}");
    }
}
