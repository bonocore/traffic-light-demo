package com.trafficlight.resource;

import com.trafficlight.model.ApiKey;
import com.trafficlight.model.CreateKeyRequest;
import com.trafficlight.security.Secured;
import com.trafficlight.service.ApiKeyService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

@Path("/api/keys")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "API Key Management", description = "Operations for creating, listing, and revoking API keys")
@org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement(name = "ApiKeyAuth")
@org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement(name = "BasicAuth")
public class ApiKeyResource {

    @Inject
    ApiKeyService apiKeyService;

    @GET
    @Secured
    @Operation(summary = "List all API keys", description = "Secured: Returns all registered API keys, active status, and metadata")
    @APIResponse(responseCode = "200", description = "List of API keys")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    public List<ApiKey> listKeys() {
        return apiKeyService.listAll();
    }

    @POST
    @Secured
    @Operation(summary = "Generate new API key", description = "Secured: Creates a new API key with a name and assigned role")
    @APIResponse(responseCode = "201", description = "Key created successfully")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    public Response createKey(CreateKeyRequest request) {
        String name = (request != null) ? request.name() : "New API Key";
        String role = (request != null) ? request.role() : "OPERATOR";
        ApiKey newKey = apiKeyService.createKey(name, role);
        return Response.status(Response.Status.CREATED).entity(newKey).build();
    }

    @DELETE
    @Path("/{id}")
    @Secured
    @Operation(summary = "Revoke API key", description = "Secured: Disables the specified API key immediately")
    @APIResponse(responseCode = "200", description = "Key revoked successfully")
    @APIResponse(responseCode = "404", description = "Key ID not found")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    public Response revokeKey(@PathParam("id") String id) {
        boolean revoked = apiKeyService.revokeKey(id);
        if (!revoked) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "Key not found with ID: " + id)).build();
        }
        return Response.ok(Map.of("message", "API key revoked successfully", "id", id)).build();
    }
}
