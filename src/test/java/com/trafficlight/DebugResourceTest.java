package com.trafficlight;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@QuarkusTest
public class DebugResourceTest {

    @Test
    public void testDebugRequestsRequiresAuth() {
        given()
            .when().get("/api/debug/requests")
            .then()
            .statusCode(401);
    }

    @Test
    public void testDebugRequestsCapturesAndClearsLogs() {
        // Clear any previous logs first
        given()
            .header("X-API-KEY", "admin-key-2026")
            .when().delete("/api/debug/requests")
            .then()
            .statusCode(200);

        // Make an authenticated POST request to /api/traffic-light/color
        given()
            .header("X-API-KEY", "admin-key-2026")
            .contentType(ContentType.JSON)
            .body("{\"color\": \"GREEN\"}")
            .when().post("/api/traffic-light/color")
            .then()
            .statusCode(200);

        // Verify the request was audited and is visible in /api/debug/requests
        given()
            .header("X-API-KEY", "admin-key-2026")
            .when().get("/api/debug/requests")
            .then()
            .statusCode(200)
            .body("[0].method", equalTo("POST"))
            .body("[0].path", equalTo("/api/traffic-light/color"))
            .body("[0].status", equalTo(200))
            .body("[0].caller", containsString("City Admin Center"))
            .body("[0].requestPayload", containsString("GREEN"))
            .body("[0].responsePayload", containsString("GREEN"));

        // Make an unauthenticated request to verify it logs as 401
        given()
            .contentType(ContentType.JSON)
            .body("{\"color\": \"RED\"}")
            .when().post("/api/traffic-light/color")
            .then()
            .statusCode(401);

        given()
            .auth().preemptive().basic("admin", "admin123")
            .when().get("/api/debug/requests")
            .then()
            .statusCode(200)
            .body("[0].status", equalTo(401))
            .body("[0].caller", containsString("Unauthorized"));

        // Clear requests
        given()
            .auth().preemptive().basic("admin", "admin123")
            .when().delete("/api/debug/requests")
            .then()
            .statusCode(200)
            .body("status", equalTo("success"));

        // Verify empty list
        given()
            .header("X-API-KEY", "admin-key-2026")
            .when().get("/api/debug/requests")
            .then()
            .statusCode(200)
            .body("size()", equalTo(0));
    }
}
