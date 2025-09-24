package com.example.soso.users.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RandomNicknameGenerator {

    private static final List<String> PREFIXES = List.of(
            "웃는", "날쌘", "조용한", "멋쟁이", "빠른", "당당한", "귀여운", "똑똑한", "수줍은", "엉뚱한",
            "활기찬", "장미를든", "신중한", "씩씩한", "천재", "용감한", "명랑한", "평화로운", "반짝이는",
            "다정한", "탐험하는", "부지런한", "무서운", "졸린", "자유로운", "튼튼한", "유쾌한", "반항하는",
            "상상하는", "재잘거리는"
    );

    private static final String FIXED_ANIMAL = "문어";

    // 이미 존재하는 닉네임을 제외하고 하나 생성 (성능 최적화된 버전)
    public static String generateUniqueNickname(java.util.function.Predicate<String> existsChecker) {
        List<String> shuffled = new ArrayList<>(PREFIXES);
        Collections.shuffle(shuffled);

        for (String prefix : shuffled) {
            String candidate = prefix + FIXED_ANIMAL;
            if (!existsChecker.test(candidate)) {
                return candidate;
            }
        }

        // 모든 기본 닉네임이 사용 중일 경우, 숫자를 붙여서 생성
        return generateWithNumber(existsChecker);
    }

    // 이전 버전과의 호환성을 위해 유지 (deprecated)
    @Deprecated(since = "1.0", forRemoval = true)
    public static String generateUniqueNickname(Set<String> existingNicknames) {
        return generateUniqueNickname(existingNicknames::contains);
    }

    private static String generateWithNumber(java.util.function.Predicate<String> existsChecker) {
        List<String> shuffled = new ArrayList<>(PREFIXES);
        Collections.shuffle(shuffled);

        for (String prefix : shuffled) {
            for (int i = 1; i <= 999; i++) {
                String candidate = prefix + FIXED_ANIMAL + i;
                if (!existsChecker.test(candidate)) {
                    return candidate;
                }
            }
        }

        throw new IllegalStateException("사용 가능한 닉네임이 없습니다.");
    }

    // 모든 닉네임 반환
    public static Set<String> allPossibleNicknames() {
        Set<String> nicknames = new HashSet<>();
        for (String prefix : PREFIXES) {
            nicknames.add(prefix + FIXED_ANIMAL);
        }
        return nicknames;
    }
}
