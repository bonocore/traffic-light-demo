package com.trafficlight.security;

import com.trafficlight.model.ApiKey;
import com.trafficlight.service.ApiKeyService;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Secured
@Provider
@Priority(Priorities.AUTHENTICATION)
public class ApiKeyFilter implements ContainerRequestFilter {

    public static final String AUTHENTICATED_KEY_PROP = "authenticatedKey";

    @Inject
    ApiKeyService apiKeyService;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String apiKey = requestContext.getHeaderString("X-API-KEY");
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = requestContext.getHeaderString("X-Api-Key");
        }

        // Also check Authorization: Bearer <key>
        if (apiKey == null || apiKey.isBlank()) {
            String authHeader = requestContext.getHeaderString("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                apiKey = authHeader.substring(7).trim();
            }
        }

        if (apiKey == null || apiKey.isBlank()) {
            abortUnauthorized(requestContext, "Missing API key. Provide header 'X-API-KEY: <key>' or 'Authorization: Bearer <key>'");
            return;
        }

        Optional<ApiKey> keyOpt = apiKeyService.validate(apiKey);
        if (keyOpt.isEmpty()) {
            abortUnauthorized(requestContext, "Invalid or revoked API key");
            return;
        }

        requestContext.setProperty(AUTHENTICATED_KEY_PROP, keyOpt.get());
    }

    private void abortUnauthorized(ContainerRequestContext requestContext, String message) {
        requestContext.abortWith(
            Response.status(Response.Status.UNAUTHORIZED)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(
                    "error", "Unauthorized",
                    "status", 401,
                    "message", message
                ))
                .build()
        );
    }
}
