package com.trafficlight.security;

import com.trafficlight.model.ApiKey;
import com.trafficlight.service.ApiKeyService;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Secured
@Provider
@Priority(Priorities.AUTHENTICATION)
public class ApiKeyFilter implements ContainerRequestFilter {

    public static final String AUTHENTICATED_KEY_PROP = "authenticatedKey";

    @Inject
    ApiKeyService apiKeyService;

    @Inject
    SecurityIdentity securityIdentity;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // 1. Check if user is already authenticated via HTTP Basic Auth (Username / Password)
        if (securityIdentity != null && !securityIdentity.isAnonymous() && securityIdentity.getPrincipal() != null) {
            String username = securityIdentity.getPrincipal().getName();
            String role = securityIdentity.hasRole("admin") ? "ADMIN" : "OPERATOR";
            requestContext.setProperty(AUTHENTICATED_KEY_PROP, new ApiKey(
                "user-" + username,
                "basic-auth",
                username + " (Basic Auth)",
                role,
                true,
                Instant.now()
            ));
            return;
        }

        // 2. Otherwise, check X-API-KEY or Bearer token
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
            abortUnauthorized(requestContext, "Missing credentials. Provide HTTP Basic Auth (User & Password) or header 'X-API-KEY: <key>'");
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
