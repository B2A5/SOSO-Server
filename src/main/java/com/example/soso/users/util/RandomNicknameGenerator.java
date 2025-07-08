package com.example.soso.users.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class RandomNicknameGenerator {

    private static final List<String> PREFIXES = List.of(
            "웃는", "날쌘", "조용한", "멋쟁이", "빠른", "당당한", "귀여운", "똑똑한", "수줍은", "엉뚱한",
            "활기찬", "장미를든", "신중한", "씩씩한", "천재", "용감한", "명랑한", "평화로운", "반짝이는",
            "다정한", "탐험하는", "부지런한", "무서운", "졸린", "자유로운", "튼튼한", "유쾌한", "반항하는",
            "상상하는", "재잘거리는"
    );

    private static final String FIXED_ANIMAL = "문어";
    private static final Random RANDOM = new Random();

    // 이미 존재하는 닉네임을 제외하고 하나 생성
    public static String generateUniqueNickname(Set<String> existingNicknames) {
        List<String> shuffled = new ArrayList<>(PREFIXES);
        Collections.shuffle(shuffled);

        for (String prefix : shuffled) {
            String candidate = prefix + FIXED_ANIMAL;
            if (!existingNicknames.contains(candidate)) {
                return candidate;
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
