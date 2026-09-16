package com.trafficlight.resource;

import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeIn;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.security.SecuritySchemes;

@OpenAPIDefinition(
    info = @Info(
        title = "Traffic Light Controller API",
        version = "1.0.0",
        description = "Secured REST APIs for controlling traffic lights (Red, Amber, Green) with real-time SSE streaming and multi-key / Basic Auth access control.",
        contact = @Contact(name = "Traffic Engineering Support")
    ),
    security = {
        @SecurityRequirement(name = "BasicAuth"),
        @SecurityRequirement(name = "ApiKeyAuth")
    }
)
@SecuritySchemes({
    @SecurityScheme(
        securitySchemeName = "BasicAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "basic",
        description = "Authenticate with username and password (default: admin / admin123, operator / operator123)"
    ),
    @SecurityScheme(
        securitySchemeName = "ApiKeyAuth",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        apiKeyName = "X-API-KEY",
        description = "Authenticate with API Key (e.g. 'admin-key-2026', 'operator-key-2026', or dynamic key)"
    )
})
public class OpenApiConfig extends Application {
}
