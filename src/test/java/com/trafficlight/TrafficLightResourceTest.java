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
    public void testSecuredStateWithValidKeySucceeds() {
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
