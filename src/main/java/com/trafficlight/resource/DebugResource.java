package com.trafficlight.resource;

import com.trafficlight.model.HttpRequestLog;
import com.trafficlight.service.RequestAuditService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.trafficlight.security.Secured;

import io.smallrye.mutiny.Multi;
import org.jboss.resteasy.reactive.RestStreamElementType;

import java.util.List;
import java.util.Map;

@Path("/api/debug")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Debugger", description = "Operations for inspecting live incoming REST requests and server diagnostic history")
@Secured
public class DebugResource {

    @Inject
    RequestAuditService requestAuditService;

    @GET
    @Path("/requests")
    @Operation(summary = "Get recent incoming REST requests", description = "Returns the last N incoming HTTP requests captured by the server audit filter")
    @APIResponse(responseCode = "200", description = "List of recent HTTP request logs")
    public List<HttpRequestLog> getRecentRequests(@QueryParam("limit") @DefaultValue("10") int limit) {
        return requestAuditService.getRecent(limit);
    }

    @GET
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    @Operation(summary = "Live incoming request audit stream (SSE)", description = "Pushes new incoming REST requests in real-time as they arrive")
    public Multi<HttpRequestLog> streamRequests() {
        return requestAuditService.getStream();
    }

    @DELETE
    @Path("/requests")
    @Operation(summary = "Clear request history", description = "Clears the in-memory ring buffer of audited HTTP requests")
    @APIResponse(responseCode = "200", description = "Request history cleared")
    public Response clearRequests() {
        requestAuditService.clear();
        return Response.ok(Map.of(
            "status", "success",
            "message", "Request audit history cleared"
        )).build();
    }
}
