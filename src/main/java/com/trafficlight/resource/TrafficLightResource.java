package com.trafficlight.resource;

import com.trafficlight.model.ApiKey;
import com.trafficlight.model.LightState;
import com.trafficlight.model.ModeChangeRequest;
import com.trafficlight.model.OperationMode;
import com.trafficlight.model.StateChangeRequest;
import com.trafficlight.model.TrafficLightStatus;
import com.trafficlight.security.ApiKeyFilter;
import com.trafficlight.security.Secured;
import com.trafficlight.service.TrafficLightService;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestStreamElementType;

@Path("/api/traffic-light")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Traffic Light Controller", description = "Operations for monitoring and controlling the traffic light")
public class TrafficLightResource {

    @Inject
    TrafficLightService trafficLightService;

    @GET
    @Path("/state")
    @Operation(summary = "Get current status", description = "Public endpoint to read active state, mode, countdown, and timestamp")
    @APIResponse(responseCode = "200", description = "Current traffic light status")
    @org.eclipse.microprofile.openapi.annotations.security.SecurityRequirements()
    public TrafficLightStatus getState() {
        return trafficLightService.getStatus();
    }

    @GET
    @Path("/mode")
    @Operation(summary = "Get current operation mode", description = "Public endpoint to get the current operating mode and list of supported modes")
    @APIResponse(responseCode = "200", description = "Current operation mode and supported modes")
    @org.eclipse.microprofile.openapi.annotations.security.SecurityRequirements()
    public Response getMode() {
        OperationMode currentMode = trafficLightService.getStatus().mode();
        return Response.ok(java.util.Map.of(
            "mode", currentMode,
            "supportedModes", java.util.List.of(com.trafficlight.model.OperationMode.values())
        )).build();
    }

    @GET
    @Path("/modes")
    @Operation(summary = "Get all supported modes", description = "Public endpoint to list all available operation modes (AUTO, MANUAL, EMERGENCY)")
    @APIResponse(responseCode = "200", description = "List of supported modes")
    @org.eclipse.microprofile.openapi.annotations.security.SecurityRequirements()
    public java.util.List<com.trafficlight.model.OperationMode> getSupportedModes() {
        return java.util.List.of(com.trafficlight.model.OperationMode.values());
    }

    @GET
    @Path("/colors")
    @Operation(summary = "Get supported light colors", description = "Public endpoint to retrieve the list of currently supported light colors/states to use when setting state (RED, AMBER, GREEN, FLASHING_AMBER, OFF)")
    @APIResponse(responseCode = "200", description = "List of supported light colors")
    @org.eclipse.microprofile.openapi.annotations.security.SecurityRequirements()
    public java.util.List<com.trafficlight.model.LightState> getSupportedColors() {
        return java.util.List.of(com.trafficlight.model.LightState.values());
    }

    @GET
    @Path("/states")
    @Operation(summary = "Get supported light states (alias for /colors)", description = "Public alias endpoint to retrieve the list of supported light states")
    @APIResponse(responseCode = "200", description = "List of supported light states")
    @org.eclipse.microprofile.openapi.annotations.security.SecurityRequirements()
    public java.util.List<com.trafficlight.model.LightState> getSupportedStates() {
        return java.util.List.of(com.trafficlight.model.LightState.values());
    }

    @GET
    @Path("/events")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    @Operation(summary = "Subscribe to live updates (SSE)", description = "Server-Sent Events stream delivering real-time state and timer updates")
    @org.eclipse.microprofile.openapi.annotations.security.SecurityRequirements()
    public Multi<TrafficLightStatus> streamEvents() {
        return trafficLightService.getEventStream();
    }

    @POST
    @Path("/state")
    @Secured
    @Operation(summary = "Set manual light state", description = "Secured: Sets the light to RED, AMBER, GREEN, FLASHING_AMBER, or OFF. Switches mode to MANUAL.")
    @org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement(name = "ApiKeyAuth")
    @org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement(name = "BasicAuth")
    @APIResponse(responseCode = "200", description = "State successfully updated")
    @APIResponse(responseCode = "401", description = "Unauthorized - Missing or invalid API key")
    public Response setState(StateChangeRequest request, @Context ContainerRequestContext context) {
        if (request == null || request.state() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\": \"State is required\"}").build();
        }
        ApiKey caller = (ApiKey) context.getProperty(ApiKeyFilter.AUTHENTICATED_KEY_PROP);
        String callerName = caller != null ? caller.name() : "Authorized Client";
        TrafficLightStatus status = trafficLightService.setLightState(request.state(), callerName);
        return Response.ok(status).build();
    }

    @POST
    @Path("/mode")
    @Secured
    @Operation(summary = "Change operation mode", description = "Secured: Sets operation mode to AUTO, MANUAL, or EMERGENCY.")
    @org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement(name = "ApiKeyAuth")
    @org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement(name = "BasicAuth")
    @APIResponse(responseCode = "200", description = "Mode successfully updated")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    public Response setMode(ModeChangeRequest request, @Context ContainerRequestContext context) {
        if (request == null || request.mode() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\": \"Mode is required\"}").build();
        }
        ApiKey caller = (ApiKey) context.getProperty(ApiKeyFilter.AUTHENTICATED_KEY_PROP);
        String callerName = caller != null ? caller.name() : "Authorized Client";
        TrafficLightStatus status = trafficLightService.setMode(request.mode(), callerName);
        return Response.ok(status).build();
    }

    @POST
    @Path("/next")
    @Secured
    @Operation(summary = "Advance to next phase", description = "Secured: Steps forward to the next logical phase in the sequence.")
    @org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement(name = "ApiKeyAuth")
    @org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement(name = "BasicAuth")
    @APIResponse(responseCode = "200", description = "Phase advanced successfully")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    public Response advanceNext(@Context ContainerRequestContext context) {
        ApiKey caller = (ApiKey) context.getProperty(ApiKeyFilter.AUTHENTICATED_KEY_PROP);
        String callerName = caller != null ? caller.name() : "Authorized Client";
        TrafficLightStatus status = trafficLightService.advanceNextPhase(callerName);
        return Response.ok(status).build();
    }
}
