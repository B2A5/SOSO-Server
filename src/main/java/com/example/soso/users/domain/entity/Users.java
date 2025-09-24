package com.example.soso.users.domain.entity;

import com.example.soso.global.time.BaseTimeEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Users extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private String id;

    private String username;
    private String nickname;
    private String email;

    @Enumerated(EnumType.STRING)
    private UserType userType;

    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    private AgeRange ageRange;

    @Enumerated(EnumType.STRING)
    private BudgetRange budget;

    @Enumerated(EnumType.STRING)
    private StartupExperience startupExperience;

    private String location;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_interests", joinColumns = @JoinColumn(name = "users_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "interest_type")
    private List<InterestType> interests;

    private String latitude;
    private String longitude;

    @Builder
    public Users(String username, String nickname, String email, UserType userType, String profileImageUrl,
                 Gender gender, AgeRange ageRange, BudgetRange budget, StartupExperience startupExperience,
                 String location, List<InterestType> interests, String latitude, String longitude) {
        this.username = username;
        this.nickname = nickname;
        this.email = email;
        this.userType = userType;
        this.profileImageUrl = profileImageUrl;
        this.gender = gender;
        this.ageRange = ageRange;
        this.budget = budget;
        this.startupExperience = startupExperience;
        this.location = location;
        this.interests = interests;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
