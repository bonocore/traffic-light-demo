package com.trafficlight;

import com.trafficlight.model.LightState;
import com.trafficlight.model.ModeChangeRequest;
import com.trafficlight.model.OperationMode;
import com.trafficlight.model.StateChangeRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@QuarkusTest
public class TrafficLightResourceTest {

    @Test
    public void testPublicGetState() {
        given()
            .when().get("/api/traffic-light/state")
            .then()
            .statusCode(200)
            .body("state", notNullValue())
            .body("mode", notNullValue());
    }

    @Test
    public void testPublicDisplayScreenReturns200() {
        given()
            .when().get("/")
            .then()
            .statusCode(200);
    }

    @Test
    public void testSecuredStateWithoutKeyReturns401() {
        given()
            .contentType(ContentType.JSON)
            .body(new StateChangeRequest(LightState.GREEN))
            .when().post("/api/traffic-light/state")
            .then()
            .statusCode(401)
            .body("error", equalTo("Unauthorized"));
    }

    @Test
    public void testSecuredStateWithInvalidKeyReturns401() {
        given()
            .header("X-API-KEY", "completely-bogus-key")
            .contentType(ContentType.JSON)
            .body(new StateChangeRequest(LightState.GREEN))
            .when().post("/api/traffic-light/state")
            .then()
            .statusCode(401)
            .body("error", equalTo("Unauthorized"));
    }

    @Test
    public void testSecuredStateWithValidApiKeySucceeds() {
        given()
            .header("X-API-KEY", "admin-key-2026")
            .contentType(ContentType.JSON)
            .body(new StateChangeRequest(LightState.GREEN))
            .when().post("/api/traffic-light/state")
            .then()
            .statusCode(200)
            .body("state", equalTo("GREEN"))
            .body("mode", equalTo("MANUAL"));
    }

    @Test
    public void testSecuredStateWithBasicAuthSucceeds() {
        given()
            .auth().preemptive().basic("admin", "admin123")
            .contentType(ContentType.JSON)
            .body(new StateChangeRequest(LightState.AMBER))
            .when().post("/api/traffic-light/state")
            .then()
            .statusCode(200)
            .body("state", equalTo("AMBER"));
    }

    @Test
    public void testControlHtmlProtectedWithoutAuthReturns401() {
        given()
            .when().get("/control.html")
            .then()
            .statusCode(401);
    }

    @Test
    public void testControlHtmlWithBasicAuthReturns200() {
        given()
            .auth().preemptive().basic("admin", "admin123")
            .when().get("/control.html")
            .then()
            .statusCode(200);
    }

    @Test
    public void testSwaggerUiProtectedWithoutAuthReturns401() {
        given()
            .when().get("/q/swagger-ui/")
            .then()
            .statusCode(401);
    }

    @Test
    public void testSwaggerUiWithBasicAuthReturns200() {
        given()
            .auth().preemptive().basic("admin", "admin123")
            .when().get("/q/swagger-ui/")
            .then()
            .statusCode(200);
    }

    @Test
    public void testSecuredModeChange() {
        given()
            .header("X-API-KEY", "operator-key-2026")
            .contentType(ContentType.JSON)
            .body(new ModeChangeRequest(OperationMode.AUTO))
            .when().post("/api/traffic-light/mode")
            .then()
            .statusCode(200)
            .body("mode", equalTo("AUTO"));
    }

    @Test
    public void testPublicGetMode() {
        given()
            .when().get("/api/traffic-light/mode")
            .then()
            .statusCode(200)
            .body("mode", notNullValue())
            .body("supportedModes", hasItems("AUTO", "MANUAL", "EMERGENCY"));
    }

    @Test
    public void testPublicGetModes() {
        given()
            .when().get("/api/traffic-light/modes")
            .then()
            .statusCode(200)
            .body("$", hasItems("AUTO", "MANUAL", "EMERGENCY"));
    }

    @Test
    public void testPublicGetColors() {
        given()
            .when().get("/api/traffic-light/colors")
            .then()
            .statusCode(200)
            .body("$", hasItems("RED", "AMBER", "GREEN", "FLASHING_AMBER", "OFF"));
    }

    @Test
    public void testPublicGetStates() {
        given()
            .when().get("/api/traffic-light/states")
            .then()
            .statusCode(200)
            .body("$", hasItems("RED", "AMBER", "GREEN", "FLASHING_AMBER", "OFF"));
    }

    @Test
    public void testSecuredModeChangeWithoutAuthReturns401() {
        given()
            .contentType(ContentType.JSON)
            .body(new ModeChangeRequest(OperationMode.MANUAL))
            .when().post("/api/traffic-light/mode")
            .then()
            .statusCode(401);
    }

    @Test
    public void testSecuredModeChangeWithBasicAuth() {
        given()
            .auth().preemptive().basic("operator", "operator123")
            .contentType(ContentType.JSON)
            .body(new ModeChangeRequest(OperationMode.EMERGENCY))
            .when().post("/api/traffic-light/mode")
            .then()
            .statusCode(200)
            .body("mode", equalTo("EMERGENCY"));
    }

    @Test
    public void testAdvanceNextPhase() {
        given()
            .header("X-API-KEY", "dispatch-key-2026")
            .contentType(ContentType.JSON)
            .when().post("/api/traffic-light/next")
            .then()
            .statusCode(200)
            .body("state", notNullValue());
    }
}
