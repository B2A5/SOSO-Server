package com.example.soso.users.domain.entity;

public enum StartupExperience {
    YES("창업 경험 유"),
    NO("창업 경험 무");

    private final String label;

    StartupExperience(String label) {
        this.label = label;
    }
}
