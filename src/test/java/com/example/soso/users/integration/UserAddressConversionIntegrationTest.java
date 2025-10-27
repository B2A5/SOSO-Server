package com.example.soso.users.integration;

import com.example.soso.users.domain.dto.UserMapper;
import com.example.soso.users.domain.dto.UserResponse;
import com.example.soso.users.domain.entity.*;
import com.example.soso.users.repository.UsersRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 사용자 주소 변환 통합 테스트
 * 시군구 코드가 도시명으로 올바르게 변환되는지 검증
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("사용자 주소 변환 통합 테스트")
class UserAddressConversionIntegrationTest {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private UserMapper userMapper;

    private Users testUser;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성 (서울특별시 강남구 - 11680)
        testUser = Users.builder()
                .username("테스트사용자")
                .nickname("테스트닉네임")
                .email("test@example.com")
                .userType(UserType.INHABITANT)
                .gender(Gender.MALE)
                .ageRange(AgeRange.TWENTIES)
                .location("11680")  // 서울특별시 강남구 코드
                .profileImageUrl("https://example.com/profile.jpg")
                .build();

        testUser = usersRepository.save(testUser);
    }

    @AfterEach
    void tearDown() {
        usersRepository.deleteAll();
    }

    @Test
    @DisplayName("유효한 시군구 코드가 도시명으로 변환됨")
    void convertValidSigunguCode() {
        // given
        Users user = usersRepository.findById(testUser.getId()).orElseThrow();

        // when
        UserResponse response = userMapper.toUserResponse(user);

        // then
        assertThat(response.getLocation()).isEqualTo("서울특별시 강남구");
        assertThat(response.getLocation()).isNotEqualTo("11680");  // 코드가 아닌 도시명
    }

    @Test
    @DisplayName("존재하지 않는 시군구 코드는 '소소 타운'으로 변환됨")
    void convertNonExistentCode() {
        // given
        testUser = Users.builder()
                .username("테스트사용자2")
                .nickname("테스트닉네임2")
                .email("test2@example.com")
                .userType(UserType.FOUNDER)
                .gender(Gender.FEMALE)
                .ageRange(AgeRange.THIRTIES)
                .location("99999")  // 존재하지 않는 코드
                .build();
        testUser = usersRepository.save(testUser);

        // when
        UserResponse response = userMapper.toUserResponse(testUser);

        // then
        assertThat(response.getLocation()).isEqualTo("소소 타운");
    }

    @Test
    @DisplayName("null 시군구 코드는 '소소 타운'으로 변환됨")
    void convertNullCode() {
        // given
        testUser = Users.builder()
                .username("테스트사용자3")
                .nickname("테스트닉네임3")
                .email("test3@example.com")
                .userType(UserType.INHABITANT)
                .gender(Gender.MALE)
                .ageRange(AgeRange.FORTIES)
                .location(null)  // null
                .build();
        testUser = usersRepository.save(testUser);

        // when
        UserResponse response = userMapper.toUserResponse(testUser);

        // then
        assertThat(response.getLocation()).isEqualTo("소소 타운");
    }

    @Test
    @DisplayName("잘못된 형식의 시군구 코드는 '소소 타운'으로 변환됨")
    void convertInvalidFormatCode() {
        // given
        testUser = Users.builder()
                .username("테스트사용자4")
                .nickname("테스트닉네임4")
                .email("test4@example.com")
                .userType(UserType.FOUNDER)
                .gender(Gender.NONE)
                .ageRange(AgeRange.FIFTIES)
                .location("abcde")  // 숫자가 아닌 형식
                .build();
        testUser = usersRepository.save(testUser);

        // when
        UserResponse response = userMapper.toUserResponse(testUser);

        // then
        assertThat(response.getLocation()).isEqualTo("소소 타운");
    }

    @Test
    @DisplayName("다양한 지역 코드가 올바르게 변환됨")
    void convertVariousCodes() {
        // given - 다양한 지역의 사용자들
        Users[] users = {
                createUser("user1", "11680", "서울특별시 강남구"),
                createUser("user2", "26350", "부산광역시 해운대구"),
                createUser("user3", "41190", "경기도 부천시"),
                createUser("user4", "50110", "제주특별자치도 제주시"),
                createUser("user5", "27110", "대구광역시 중구")
        };

        // when & then
        for (int i = 0; i < users.length; i++) {
            Users savedUser = usersRepository.save(users[i]);
            UserResponse response = userMapper.toUserResponse(savedUser);

            String expectedLocation = switch (i) {
                case 0 -> "서울특별시 강남구";
                case 1 -> "부산광역시 해운대구";
                case 2 -> "경기도 부천시";
                case 3 -> "제주특별자치도 제주시";
                case 4 -> "대구광역시 중구";
                default -> throw new IllegalStateException();
            };

            assertThat(response.getLocation())
                    .withFailMessage("사용자 %s의 주소 변환이 잘못되었습니다. 예상: %s, 실제: %s",
                            savedUser.getUsername(), expectedLocation, response.getLocation())
                    .isEqualTo(expectedLocation);
        }
    }

    @Test
    @DisplayName("UserSummary에서도 시군구 코드가 올바르게 변환됨")
    void convertInUserSummary() {
        // given
        Users user = usersRepository.findById(testUser.getId()).orElseThrow();

        // when
        var summary = userMapper.toUserSummary(user);

        // then
        assertThat(summary.location()).isEqualTo("서울특별시 강남구");
        assertThat(summary.userId()).isEqualTo(user.getId());
        assertThat(summary.nickname()).isEqualTo(user.getNickname());
    }

    @Test
    @DisplayName("다양한 엣지 케이스 시군구 코드 처리")
    void handleEdgeCases() {
        // given - 엣지 케이스 코드들
        String[] edgeCaseCodes = {
                "",          // 빈 문자열
                "   ",       // 공백
                "123",       // 3자리
                "1234567",   // 7자리
                "00000",     // 0으로만 구성
                "ㄱㄴㄷㄹㅁ"   // 한글
        };

        // when & then
        for (String code : edgeCaseCodes) {
            Users user = createUser("edge_" + code, code, "소소 타운");
            Users savedUser = usersRepository.save(user);
            UserResponse response = userMapper.toUserResponse(savedUser);

            assertThat(response.getLocation())
                    .withFailMessage("코드 '%s'가 '소소 타운'으로 변환되지 않았습니다. 실제: %s",
                            code, response.getLocation())
                    .isEqualTo("소소 타운");
        }
    }

    /**
     * 테스트용 사용자 생성 헬퍼 메서드
     */
    private Users createUser(String username, String locationCode, String expectedAddress) {
        return Users.builder()
                .username(username)
                .nickname(username + "_nick")
                .email(username + "@example.com")
                .userType(UserType.INHABITANT)
                .gender(Gender.MALE)
                .ageRange(AgeRange.TWENTIES)
                .location(locationCode)
                .build();
    }
}
