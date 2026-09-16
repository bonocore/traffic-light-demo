package com.trafficlight;

import com.trafficlight.model.CreateKeyRequest;
import com.trafficlight.model.LightState;
import com.trafficlight.model.StateChangeRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@QuarkusTest
public class ApiKeyResourceTest {

    @Test
    public void testListKeysWithoutAuthReturns401() {
        given()
            .when().get("/api/keys")
            .then()
            .statusCode(401);
    }

    @Test
    public void testListKeysWithAuthReturnsDefaultKeys() {
        given()
            .header("X-API-KEY", "admin-key-2026")
            .when().get("/api/keys")
            .then()
            .statusCode(200)
            .body("size()", not(0));
    }

    @Test
    public void testCreateKeyUseItAndRevoke() {
        // 1. Create a new key
        Response createResp = given()
            .header("X-API-KEY", "admin-key-2026")
            .contentType(ContentType.JSON)
            .body(new CreateKeyRequest("Highway Patrol 12", "EMERGENCY"))
            .when().post("/api/keys")
            .then()
            .statusCode(201)
            .body("name", equalTo("Highway Patrol 12"))
            .body("role", equalTo("EMERGENCY"))
            .body("enabled", equalTo(true))
            .extract().response();

        String newKeyId = createResp.path("id");
        String newKeyToken = createResp.path("key");

        // 2. Use the new key to change state -> Should SUCCEED (200)
        given()
            .header("X-API-KEY", newKeyToken)
            .contentType(ContentType.JSON)
            .body(new StateChangeRequest(LightState.AMBER))
            .when().post("/api/traffic-light/state")
            .then()
            .statusCode(200)
            .body("state", equalTo("AMBER"));

        // 3. Revoke the key
        given()
            .header("X-API-KEY", "admin-key-2026")
            .when().delete("/api/keys/" + newKeyId)
            .then()
            .statusCode(200)
            .body("message", containsString("revoked successfully"));

        // 4. Try using the revoked key again -> Must be REJECTED (401)
        given()
            .header("X-API-KEY", newKeyToken)
            .contentType(ContentType.JSON)
            .body(new StateChangeRequest(LightState.GREEN))
            .when().post("/api/traffic-light/state")
            .then()
            .statusCode(401)
            .body("error", equalTo("Unauthorized"));
    }
}
