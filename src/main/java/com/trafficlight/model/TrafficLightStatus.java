package com.trafficlight.model;

import java.time.Instant;

public record TrafficLightStatus(
    LightState state,
    OperationMode mode,
    int remainingSeconds,
    int cycleSeconds,
    Instant lastChanged,
    String message
) {}
