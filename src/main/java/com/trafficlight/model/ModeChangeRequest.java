package com.trafficlight.model;

import com.fasterxml.jackson.annotation.JsonAlias;

public record ModeChangeRequest(
    OperationMode mode,

    @JsonAlias({"color", "state", "lightColor", "lightState"})
    LightState state
) {
    public ModeChangeRequest(OperationMode mode) {
        this(mode, null);
    }
}
