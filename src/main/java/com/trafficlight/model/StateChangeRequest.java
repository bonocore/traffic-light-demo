package com.trafficlight.model;

import com.fasterxml.jackson.annotation.JsonAlias;

public record StateChangeRequest(
    @JsonAlias({"color", "lightColor", "lightState"})
    LightState state,

    OperationMode mode
) {
    public StateChangeRequest(LightState state) {
        this(state, null);
    }
}
