package com.trafficlight.security;

import com.trafficlight.model.ApiKey;
import com.trafficlight.model.HttpRequestLog;
import com.trafficlight.service.RequestAuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.security.identity.SecurityIdentity;
import io.vertx.ext.web.RoutingContext;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Provider
@Priority(Priorities.AUTHENTICATION - 100)
public class RequestAuditFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String START_TIME_PROP = "audit_request_start_time";

    @Inject
    RequestAuditService requestAuditService;

    @Inject
    SecurityIdentity securityIdentity;

    @Inject
    RoutingContext routingContext;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String path = requestContext.getUriInfo().getPath();
        if (path == null) {
            path = "";
        }

        // Only audit /api/* endpoints, and skip the debugger request endpoint itself to avoid poll clutter
        if (!path.startsWith("api/") && !path.startsWith("/api/")) {
            return;
        }
        if (path.contains("api/debug/requests")) {
            return;
        }

        requestContext.setProperty(START_TIME_PROP, System.currentTimeMillis());

        if (requestContext.hasEntity()) {
            java.io.InputStream is = requestContext.getEntityStream();
            if (is != null) {
                byte[] bytes = is.readAllBytes();
                if (bytes.length > 0) {
                    String body = new String(bytes, java.nio.charset.StandardCharsets.UTF_8).trim();
                    if (body.length() > 500) {
                        body = body.substring(0, 500) + "... [truncated]";
                    }
                    requestContext.setProperty("audit_request_payload", body);
                }
                requestContext.setEntityStream(new java.io.ByteArrayInputStream(bytes));
            }
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        Object startTimeObj = requestContext.getProperty(START_TIME_PROP);
        if (startTimeObj == null) {
            return;
        }

        long startTime = (long) startTimeObj;
        long durationMs = Math.max(0, System.currentTimeMillis() - startTime);

        String path = "/" + requestContext.getUriInfo().getPath().replaceAll("^/+", "");
        String method = requestContext.getMethod();
        String query = requestContext.getUriInfo().getRequestUri().getQuery();
        int status = responseContext.getStatus();

        // Determine caller identity
        String caller = resolveCaller(requestContext, status);

        // Safely extract request payload and response payload if available
        String requestPayload = extractPayload(requestContext);
        String responsePayload = extractResponsePayload(responseContext);

        HttpRequestLog logEntry = new HttpRequestLog(
            "req-" + UUID.randomUUID().toString().substring(0, 8),
            Instant.now(),
            method,
            path,
            query,
            caller,
            status,
            durationMs,
            requestPayload,
            responsePayload
        );

        requestAuditService.record(logEntry);
    }

    private String resolveCaller(ContainerRequestContext requestContext, int status) {
        // 1. From ApiKeyFilter authenticated property
        Object authenticatedKey = requestContext.getProperty(ApiKeyFilter.AUTHENTICATED_KEY_PROP);
        if (authenticatedKey instanceof ApiKey apiKey) {
            return apiKey.name() + " [" + apiKey.role() + "]";
        }

        // 2. From Quarkus SecurityIdentity (Basic Auth)
        if (securityIdentity != null && !securityIdentity.isAnonymous() && securityIdentity.getPrincipal() != null) {
            return securityIdentity.getPrincipal().getName() + " [Basic Auth]";
        }

        // 3. Fallback check for credentials in headers
        String apiKeyHeader = requestContext.getHeaderString("X-API-KEY");
        if (apiKeyHeader == null) {
            apiKeyHeader = requestContext.getHeaderString("X-Api-Key");
        }
        if (apiKeyHeader != null && !apiKeyHeader.isBlank()) {
            return status == 401 ? "Unauthorized Key (" + apiKeyHeader + ")" : "API Key (" + apiKeyHeader + ")";
        }

        String authHeader = requestContext.getHeaderString("Authorization");
        if (authHeader != null && authHeader.startsWith("Basic ")) {
            return "Basic Auth (Failed / Invalid)";
        }

        if (status == 401) {
            return "Unauthorized (Missing Credentials)";
        }

        return "Public / Unauthenticated";
    }

    private String extractPayload(ContainerRequestContext requestContext) {
        Object saved = requestContext.getProperty("audit_request_payload");
        if (saved instanceof String s && !s.isBlank()) {
            return s;
        }

        try {
            if (routingContext != null && routingContext.body() != null) {
                String body = routingContext.body().asString();
                if (body != null && !body.isBlank()) {
                    body = body.trim();
                    if (body.length() > 500) {
                        return body.substring(0, 500) + "... [truncated]";
                    }
                    return body;
                }
            }
        } catch (Exception ignored) {
            // Non-fatal if body extraction fails
        }
        return null;
    }

    private String extractResponsePayload(ContainerResponseContext responseContext) {
        if (!responseContext.hasEntity()) {
            return null;
        }
        Object entity = responseContext.getEntity();
        if (entity == null) {
            return null;
        }

        try {
            if (entity instanceof String s) {
                s = s.trim();
                return s.length() > 1000 ? s.substring(0, 1000) + "... [truncated]" : s;
            }
            if (objectMapper != null) {
                String json = objectMapper.writeValueAsString(entity);
                return json.length() > 1000 ? json.substring(0, 1000) + "... [truncated]" : json;
            }
            return String.valueOf(entity);
        } catch (Exception ignored) {
            return String.valueOf(entity);
        }
    }
}
