package com.example.soso.users.domain.entity;

import java.util.List;

public record InterestRequest(
        List<InterestType> interests
) {}
