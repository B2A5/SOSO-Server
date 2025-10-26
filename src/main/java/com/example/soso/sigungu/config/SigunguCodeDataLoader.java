package com.example.soso.sigungu.config;

import com.example.soso.sigungu.domain.entity.SigunguCode;
import com.example.soso.sigungu.repository.SigunguCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 시군구 코드 CSV 데이터 로더
 *
 * 애플리케이션 시작 시 CSV 파일에서 시군구 코드 데이터를 읽어 DB에 저장합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SigunguCodeDataLoader implements CommandLineRunner {

    private final SigunguCodeRepository sigunguCodeRepository;

    private static final String CSV_FILE_PATH = "data/sigunguCode.csv";

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 이미 데이터가 있으면 로드하지 않음
        long count = sigunguCodeRepository.count();
        if (count > 0) {
            log.info("시군구 코드 데이터가 이미 존재합니다. (총 {}건) 로드를 건너뜁니다.", count);
            return;
        }

        log.info("시군구 코드 CSV 파일 로드 시작: {}", CSV_FILE_PATH);

        try {
            ClassPathResource resource = new ClassPathResource(CSV_FILE_PATH);
            List<SigunguCode> sigunguCodes = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

                // 헤더 라인 건너뛰기
                String headerLine = reader.readLine();
                log.debug("CSV 헤더: {}", headerLine);

                String line;
                int lineNumber = 1;

                while ((line = reader.readLine()) != null) {
                    lineNumber++;

                    // 빈 줄 건너뛰기
                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    try {
                        SigunguCode sigunguCode = parseCsvLine(line);
                        sigunguCodes.add(sigunguCode);
                    } catch (Exception e) {
                        log.warn("CSV 라인 {} 파싱 실패: {} - 에러: {}", lineNumber, line, e.getMessage());
                    }
                }
            }

            // 일괄 저장
            if (!sigunguCodes.isEmpty()) {
                sigunguCodeRepository.saveAll(sigunguCodes);
                log.info("시군구 코드 데이터 로드 완료: 총 {}건", sigunguCodes.size());
            } else {
                log.warn("로드할 시군구 코드 데이터가 없습니다.");
            }

        } catch (Exception e) {
            log.error("시군구 코드 CSV 파일 로드 실패: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * CSV 라인을 파싱하여 SigunguCode 엔티티로 변환
     *
     * @param line CSV 라인 (형식: code,sido,sigungu,fullName)
     * @return SigunguCode 엔티티
     */
    private SigunguCode parseCsvLine(String line) {
        // CSV 파싱 (간단한 split 사용, 실제로는 더 견고한 파서 사용 권장)
        String[] parts = line.split(",", -1);

        if (parts.length != 4) {
            throw new IllegalArgumentException("잘못된 CSV 형식: " + line);
        }

        String code = parts[0].trim();
        String sido = parts[1].trim();
        String sigungu = parts[2].trim();
        String fullName = parts[3].trim();

        // 유효성 검증
        if (!SigunguCode.isValidCode(code)) {
            throw new IllegalArgumentException("유효하지 않은 시군구 코드: " + code);
        }

        return SigunguCode.builder()
                .code(code)
                .sido(sido)
                .sigungu(sigungu)
                .fullName(fullName)
                .build();
    }
}
