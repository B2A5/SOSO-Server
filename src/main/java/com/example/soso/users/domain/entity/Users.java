package com.example.soso.users.domain.entity;

import com.example.soso.global.time.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Users extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String nickName;

    private String phoneNumber;

    private String email;

    @Enumerated(EnumType.STRING)
    private UserType userType;

    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private LocalDate birthDate;

    private String location;

    private String latitude;
    private String longitude;

    @Builder
    public Users(String username, String nickName, String phoneNumber, String email, String profileImageUrl, UserType userType,
                 Gender gender, LocalDate birthDate, String location, String latitude, String longitude) {
        this.username = username;
        this.nickName = nickName;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.userType = userType;
        this.gender = gender;
        this.birthDate = birthDate;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
