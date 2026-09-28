package com.trafficlight.model;

import java.time.Instant;

public record HttpRequestLog(
    String id,
    Instant timestamp,
    String method,
    String path,
    String query,
    String caller,
    int status,
    long durationMs,
    String payload
) {}
