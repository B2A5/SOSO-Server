package com.example.soso.sigungu.integration;

import com.example.soso.sigungu.domain.entity.SigunguCode;
import com.example.soso.sigungu.repository.SigunguCodeRepository;
import com.example.soso.sigungu.service.SigunguCodeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 시군구 코드 통합 테스트
 * CSV 데이터 로드 및 실제 DB 조회 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("시군구 코드 통합 테스트")
class SigunguCodeIntegrationTest {

    @Autowired
    private SigunguCodeRepository sigunguCodeRepository;

    @Autowired
    private SigunguCodeService sigunguCodeService;

    @Test
    @DisplayName("CSV 파일에서 시군구 코드 데이터가 정상적으로 로드됨")
    void csvDataLoaded() {
        // when
        long count = sigunguCodeRepository.count();

        // then
        assertThat(count).isGreaterThan(0);
        System.out.println("로드된 시군구 코드 개수: " + count);
    }

    @Test
    @DisplayName("서울특별시 강남구 코드 조회")
    void findSeoulGangnam() {
        // given
        String code = "11680";

        // when
        Optional<SigunguCode> result = sigunguCodeRepository.findByCode(code);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getSido()).isEqualTo("서울특별시");
        assertThat(result.get().getSigungu()).isEqualTo("강남구");
        assertThat(result.get().getFullName()).isEqualTo("서울특별시 강남구");
    }

    @Test
    @DisplayName("부산광역시 해운대구 코드 조회")
    void findBusanHaeundae() {
        // given
        String code = "26350";

        // when
        Optional<SigunguCode> result = sigunguCodeRepository.findByCode(code);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getSido()).isEqualTo("부산광역시");
        assertThat(result.get().getSigungu()).isEqualTo("해운대구");
        assertThat(result.get().getFullName()).isEqualTo("부산광역시 해운대구");
    }

    @Test
    @DisplayName("제주특별자치도 제주시 코드 조회")
    void findJejuCity() {
        // given
        String code = "50110";

        // when
        Optional<SigunguCode> result = sigunguCodeRepository.findByCode(code);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getSido()).isEqualTo("제주특별자치도");
        assertThat(result.get().getSigungu()).isEqualTo("제주시");
        assertThat(result.get().getFullName()).isEqualTo("제주특별자치도 제주시");
    }

    @Test
    @DisplayName("서비스를 통한 시군구 코드 변환 - 정상 케이스")
    void convertValidCode() {
        // given
        String seoulGangnamCode = "11680";
        String busanHaeundaeCode = "26350";

        // when
        String seoulResult = sigunguCodeService.convertToAddress(seoulGangnamCode);
        String busanResult = sigunguCodeService.convertToAddress(busanHaeundaeCode);

        // then
        assertThat(seoulResult).isEqualTo("서울특별시 강남구");
        assertThat(busanResult).isEqualTo("부산광역시 해운대구");
    }

    @Test
    @DisplayName("서비스를 통한 시군구 코드 변환 - 존재하지 않는 코드")
    void convertNonExistentCode() {
        // given
        String invalidCode = "99999";

        // when
        String result = sigunguCodeService.convertToAddress(invalidCode);

        // then
        assertThat(result).isEqualTo("소소 타운");
    }

    @Test
    @DisplayName("서비스를 통한 시군구 코드 변환 - null")
    void convertNullCode() {
        // when
        String result = sigunguCodeService.convertToAddress(null);

        // then
        assertThat(result).isEqualTo("소소 타운");
    }

    @Test
    @DisplayName("서비스를 통한 시군구 코드 변환 - 잘못된 형식")
    void convertInvalidFormat() {
        // given
        String[] invalidCodes = {"123", "abcde", "12345678", "1234a"};

        // when & then
        for (String code : invalidCodes) {
            String result = sigunguCodeService.convertToAddress(code);
            assertThat(result).isEqualTo("소소 타운");
        }
    }

    @Test
    @DisplayName("전국 주요 도시 시군구 코드 확인")
    void verifyMajorCities() {
        // given - 전국 주요 도시 코드
        String[][] majorCities = {
                {"11110", "서울특별시 종로구"},
                {"11680", "서울특별시 강남구"},
                {"26110", "부산광역시 중구"},
                {"27110", "대구광역시 중구"},
                {"28110", "인천광역시 중구"},
                {"29110", "광주광역시 동구"},
                {"30110", "대전광역시 동구"},
                {"31110", "울산광역시 중구"},
                {"36110", "세종특별자치시"},
                {"41111", "경기도 수원시 장안구"},
                {"42110", "강원특별자치도 춘천시"},
                {"43111", "충청북도 청주시 상당구"},
                {"44131", "충청남도 천안시 동남구"},
                {"45111", "전북특별자치도 전주시 완산구"},
                {"46110", "전라남도 목포시"},
                {"47111", "경상북도 포항시 남구"},
                {"48121", "경상남도 창원시 의창구"},
                {"50110", "제주특별자치도 제주시"}
        };

        // when & then
        for (String[] city : majorCities) {
            String code = city[0];
            String expectedName = city[1];

            String result = sigunguCodeService.convertToAddress(code);
            assertThat(result)
                    .withFailMessage("코드 %s의 변환 결과가 예상과 다릅니다. 예상: %s, 실제: %s",
                            code, expectedName, result)
                    .isEqualTo(expectedName);
        }
    }

    @Test
    @DisplayName("convertToAddressSafe는 예외 발생 없이 항상 결과 반환")
    void convertToAddressSafeNeverThrows() {
        // given
        String[] testCodes = {"11680", "99999", null, "", "invalid", "123"};

        // when & then - 예외 발생하지 않아야 함
        for (String code : testCodes) {
            String result = sigunguCodeService.convertToAddressSafe(code);
            assertThat(result).isNotNull();
            assertThat(result).isNotEmpty();
        }
    }
}
