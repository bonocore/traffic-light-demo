package com.trafficlight.model;

import java.time.Instant;

public record ApiKey(
    String id,
    String key,
    String name,
    String role,
    boolean enabled,
    Instant createdAt
) {}
