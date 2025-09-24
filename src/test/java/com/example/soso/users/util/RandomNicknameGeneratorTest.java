package com.example.soso.users.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("랜덤 닉네임 생성기 테스트")
class RandomNicknameGeneratorTest {

    @Test
    @DisplayName("기본 닉네임 생성 - 사용 가능한 닉네임이 있는 경우")
    void generateUniqueNickname_WhenAvailableNicknamesExist() {
        // given
        Predicate<String> neverExists = nickname -> false;

        // when
        String nickname = RandomNicknameGenerator.generateUniqueNickname(neverExists);

        // then
        assertThat(nickname).isNotNull();
        assertThat(nickname).endsWith("문어");
        assertThat(nickname.length()).isGreaterThan(2);
    }

    @Test
    @DisplayName("모든 기본 닉네임이 사용 중일 때 숫자 붙인 닉네임 생성")
    void generateUniqueNickname_WhenAllBasicNicknamesAreTaken() {
        // given
        Set<String> allBasicNicknames = RandomNicknameGenerator.allPossibleNicknames();
        Predicate<String> basicNicknamesExist = allBasicNicknames::contains;

        // when
        String nickname = RandomNicknameGenerator.generateUniqueNickname(basicNicknamesExist);

        // then
        assertThat(nickname).isNotNull();
        assertThat(nickname).endsWith("문어1");
        assertThat(nickname).matches(".*문어\\d+$");
    }

    @Test
    @DisplayName("특정 닉네임들이 이미 사용 중인 경우 사용 가능한 닉네임 생성")
    void generateUniqueNickname_WhenSomeNicknamesAreTaken() {
        // given
        Set<String> takenNicknames = Set.of("웃는문어", "날쌘문어", "조용한문어");
        Predicate<String> isNicknameTaken = takenNicknames::contains;

        // when
        String nickname = RandomNicknameGenerator.generateUniqueNickname(isNicknameTaken);

        // then
        assertThat(nickname).isNotNull();
        assertThat(nickname).endsWith("문어");
        assertThat(takenNicknames).doesNotContain(nickname);
    }

    @Test
    @DisplayName("모든 가능한 닉네임이 사용 중일 때 예외 발생")
    void generateUniqueNickname_WhenAllPossibleNicknamesAreTaken() {
        // given
        Predicate<String> allExist = nickname -> true;

        // when & then
        assertThatThrownBy(() -> RandomNicknameGenerator.generateUniqueNickname(allExist))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("사용 가능한 닉네임이 없습니다.");
    }

    @Test
    @DisplayName("생성되는 닉네임은 항상 유니크해야 함")
    void generateUniqueNickname_ShouldAlwaysReturnUniqueNames() {
        // given
        Set<String> generatedNicknames = new HashSet<>();
        Predicate<String> isAlreadyGenerated = generatedNicknames::contains;

        // when
        for (int i = 0; i < 10; i++) {
            String nickname = RandomNicknameGenerator.generateUniqueNickname(isAlreadyGenerated);
            generatedNicknames.add(nickname);
        }

        // then
        assertThat(generatedNicknames).hasSize(10);
        generatedNicknames.forEach(nickname -> {
            assertThat(nickname.endsWith("문어") || nickname.matches(".*문어\\d+$")).isTrue();
        });
    }

    @Test
    @DisplayName("모든 가능한 기본 닉네임 반환")
    void allPossibleNicknames_ReturnsAllBasicNicknames() {
        // when
        Set<String> allNicknames = RandomNicknameGenerator.allPossibleNicknames();

        // then
        assertThat(allNicknames).isNotEmpty();
        assertThat(allNicknames).allMatch(nickname -> nickname.endsWith("문어"));
        assertThat(allNicknames).allMatch(nickname -> !nickname.matches(".*문어\\d+$"));

        // 예상되는 몇 가지 닉네임들이 포함되어 있는지 확인
        assertThat(allNicknames).containsAnyOf("웃는문어", "날쌘문어", "조용한문어");
    }

    @Test
    @DisplayName("deprecated 메서드 호환성 테스트")
    @SuppressWarnings("deprecation")
    void generateUniqueNickname_LegacyMethod_ShouldWork() {
        // given
        Set<String> takenNicknames = Set.of("웃는문어", "날쌘문어");

        // when
        String nickname = RandomNicknameGenerator.generateUniqueNickname(takenNicknames);

        // then
        assertThat(nickname).isNotNull();
        assertThat(nickname).endsWith("문어");
        assertThat(takenNicknames).doesNotContain(nickname);
    }

    @Test
    @DisplayName("빈 Set으로 호출 시 정상 동작")
    @SuppressWarnings("deprecation")
    void generateUniqueNickname_WithEmptySet_ShouldWork() {
        // given
        Set<String> emptySet = new HashSet<>();

        // when
        String nickname = RandomNicknameGenerator.generateUniqueNickname(emptySet);

        // then
        assertThat(nickname).isNotNull();
        assertThat(nickname).endsWith("문어");
    }
}