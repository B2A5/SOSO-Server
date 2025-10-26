package com.example.soso.sigungu.repository;

import com.example.soso.sigungu.domain.entity.SigunguCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 시군구 코드 리포지토리
 */
@Repository
public interface SigunguCodeRepository extends JpaRepository<SigunguCode, String> {

    /**
     * 시군구 코드로 지역 정보 조회
     *
     * @param code 5자리 시군구 코드
     * @return 지역 정보 (Optional)
     */
    Optional<SigunguCode> findByCode(String code);
}
