package com.trafficlight.model;

import com.fasterxml.jackson.annotation.JsonAlias;
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
    @JsonAlias({"payload", "requestBody"})
    String requestPayload,
    String responsePayload
) {}
