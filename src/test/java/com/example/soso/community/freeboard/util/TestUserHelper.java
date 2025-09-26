package com.example.soso.community.freeboard.util;

import com.example.soso.users.domain.entity.*;
import com.example.soso.users.repository.UsersRepository;
import com.example.soso.global.jwt.JwtProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TestUserHelper {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private JwtProvider jwtProvider;

    public TestUser createFounderUser() {
        Users user = Users.builder()
                .username("testFounder_" + System.currentTimeMillis())
                .nickname("창업가문어" + (int)(Math.random() * 1000))
                .email("founder" + System.currentTimeMillis() + "@example.com")
                .userType(UserType.FOUNDER)
                .profileImageUrl("https://example.com/founder-profile.jpg")
                .gender(Gender.MALE)
                .ageRange(AgeRange.THIRTIES)
                .budget(BudgetRange.THOUSANDS_3000_5000)
                .startupExperience(StartupExperience.YES)
                .location("11680") // 강남구
                .interests(List.of(InterestType.ACCOMMODATION_FOOD, InterestType.MANUFACTURING))
                .latitude(null)
                .longitude(null)
                .build();

        Users savedUser = usersRepository.save(user);
        usersRepository.flush();

        String accessToken = jwtProvider.generateAccessToken(savedUser.getId());
        String authHeader = "Bearer " + accessToken;

        return new TestUser(savedUser, accessToken, authHeader);
    }

    public TestUser createInhabitantUser() {
        Users user = Users.builder()
                .username("testInhabitant_" + System.currentTimeMillis())
                .nickname("거주민문어" + (int)(Math.random() * 1000))
                .email("inhabitant" + System.currentTimeMillis() + "@example.com")
                .userType(UserType.INHABITANT)
                .profileImageUrl("https://example.com/inhabitant-profile.jpg")
                .gender(Gender.FEMALE)
                .ageRange(AgeRange.TWENTIES)
                .budget(null) // INHABITANT는 예산 정보 없음
                .startupExperience(null) // INHABITANT는 창업 경험 정보 없음
                .location("11110") // 종로구
                .interests(null) // INHABITANT는 관심 업종 없음
                .latitude(null)
                .longitude(null)
                .build();

        Users savedUser = usersRepository.save(user);
        usersRepository.flush();

        String accessToken = jwtProvider.generateAccessToken(savedUser.getId());
        String authHeader = "Bearer " + accessToken;

        return new TestUser(savedUser, accessToken, authHeader);
    }

    public static class TestUser {
        private final Users user;
        private final String accessToken;
        private final String authHeader;

        public TestUser(Users user, String accessToken, String authHeader) {
            this.user = user;
            this.accessToken = accessToken;
            this.authHeader = authHeader;
        }

        public Users getUser() { return user; }
        public String getAccessToken() { return accessToken; }
        public String getAuthHeader() { return authHeader; }
        public String getUserId() { return user.getId(); }
        public String getNickname() { return user.getNickname(); }
        public UserType getUserType() { return user.getUserType(); }
        public String getLocation() { return user.getLocation(); }
    }
}