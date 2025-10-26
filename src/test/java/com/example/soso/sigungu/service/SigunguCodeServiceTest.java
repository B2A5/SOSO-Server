package com.example.soso.sigungu.service;

import com.example.soso.sigungu.domain.entity.SigunguCode;
import com.example.soso.sigungu.repository.SigunguCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("시군구 코드 서비스 테스트")
class SigunguCodeServiceTest {

    @Mock
    private SigunguCodeRepository sigunguCodeRepository;

    @InjectMocks
    private SigunguCodeService sigunguCodeService;

    private SigunguCode seoulGangnam;
    private SigunguCode busanHaeundae;

    @BeforeEach
    void setUp() {
        seoulGangnam = SigunguCode.builder()
                .code("11680")
                .sido("서울특별시")
                .sigungu("강남구")
                .fullName("서울특별시 강남구")
                .build();

        busanHaeundae = SigunguCode.builder()
                .code("26350")
                .sido("부산광역시")
                .sigungu("해운대구")
                .fullName("부산광역시 해운대구")
                .build();
    }

    @Test
    @DisplayName("유효한 시군구 코드를 도시명으로 변환")
    void convertValidSigunguCode() {
        // given
        when(sigunguCodeRepository.findByCode("11680")).thenReturn(Optional.of(seoulGangnam));

        // when
        String result = sigunguCodeService.convertToAddress("11680");

        // then
        assertThat(result).isEqualTo("서울특별시 강남구");
    }

    @Test
    @DisplayName("존재하지 않는 시군구 코드는 '소소 타운' 반환")
    void convertNonExistentCode() {
        // given
        when(sigunguCodeRepository.findByCode("99999")).thenReturn(Optional.empty());

        // when
        String result = sigunguCodeService.convertToAddress("99999");

        // then
        assertThat(result).isEqualTo("소소 타운");
    }

    @Test
    @DisplayName("null 시군구 코드는 '소소 타운' 반환")
    void convertNullCode() {
        // when
        String result = sigunguCodeService.convertToAddress(null);

        // then
        assertThat(result).isEqualTo("소소 타운");
    }

    @Test
    @DisplayName("빈 문자열 시군구 코드는 '소소 타운' 반환")
    void convertEmptyCode() {
        // when
        String result = sigunguCodeService.convertToAddress("");

        // then
        assertThat(result).isEqualTo("소소 타운");
    }

    @Test
    @DisplayName("공백 문자열 시군구 코드는 '소소 타운' 반환")
    void convertBlankCode() {
        // when
        String result = sigunguCodeService.convertToAddress("   ");

        // then
        assertThat(result).isEqualTo("소소 타운");
    }

    @Test
    @DisplayName("5자리가 아닌 시군구 코드는 '소소 타운' 반환")
    void convertInvalidLengthCode() {
        // when
        String result1 = sigunguCodeService.convertToAddress("123");
        String result2 = sigunguCodeService.convertToAddress("1234567");

        // then
        assertThat(result1).isEqualTo("소소 타운");
        assertThat(result2).isEqualTo("소소 타운");
    }

    @Test
    @DisplayName("숫자가 아닌 시군구 코드는 '소소 타운' 반환")
    void convertNonNumericCode() {
        // when
        String result = sigunguCodeService.convertToAddress("abcde");

        // then
        assertThat(result).isEqualTo("소소 타운");
    }

    @Test
    @DisplayName("여러 시군구 코드를 변환")
    void convertMultipleCodes() {
        // given
        when(sigunguCodeRepository.findByCode("11680")).thenReturn(Optional.of(seoulGangnam));
        when(sigunguCodeRepository.findByCode("26350")).thenReturn(Optional.of(busanHaeundae));

        // when
        String result1 = sigunguCodeService.convertToAddress("11680");
        String result2 = sigunguCodeService.convertToAddress("26350");

        // then
        assertThat(result1).isEqualTo("서울특별시 강남구");
        assertThat(result2).isEqualTo("부산광역시 해운대구");
    }

    @Test
    @DisplayName("convertToAddressSafe는 예외 발생 시에도 기본값 반환")
    void convertToAddressSafe_withException() {
        // given
        when(sigunguCodeRepository.findByCode(anyString())).thenThrow(new RuntimeException("DB Error"));

        // when
        String result = sigunguCodeService.convertToAddressSafe("11680");

        // then
        assertThat(result).isEqualTo("소소 타운");
    }

    @Test
    @DisplayName("getDefaultLocation은 '소소 타운' 반환")
    void getDefaultLocation() {
        // when
        String result = sigunguCodeService.getDefaultLocation();

        // then
        assertThat(result).isEqualTo("소소 타운");
    }
}
