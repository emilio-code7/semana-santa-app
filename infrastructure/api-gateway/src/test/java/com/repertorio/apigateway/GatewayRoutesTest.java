package com.repertorio.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:9999/realms/test",
    "eureka.client.enabled=false"
})
class GatewayRoutesTest {

    private static final String HID = "11111111-1111-1111-1111-111111111111";
    private static final String PID = "22222222-2222-2222-2222-222222222222";
    private static final String PASO_ID = "33333333-3333-3333-3333-333333333333";

    @Autowired
    private RouteLocator routeLocator;

    private String firstMatchingRouteId(String path) {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(path));
        return routeLocator.getRoutes().collectList().block().stream()
            .filter(route -> Boolean.TRUE.equals(Mono.from(route.getPredicate().apply(exchange)).block()))
            .map(Route::getId)
            .findFirst()
            .orElse(null);
    }

    @Test
    void monthOnePathsRouteToProcesionService() {
        assertThat(firstMatchingRouteId("/api/hermandades/" + HID + "/procesiones/" + PID + "/pasos"))
            .isEqualTo("procesion-pasos");
        assertThat(firstMatchingRouteId("/api/hermandades/" + HID + "/procesiones/" + PID + "/pasos/" + PASO_ID))
            .isEqualTo("procesion-pasos");
        assertThat(firstMatchingRouteId("/api/hermandades/" + HID + "/procesiones/" + PID + "/route"))
            .isEqualTo("procesion-route");
        assertThat(firstMatchingRouteId("/api/hermandades/" + HID + "/procesiones/" + PID + "/plan/finalize"))
            .isEqualTo("procesion-plan");
    }

    @Test
    void crucetaPathsRouteToRepertorioService() {
        String base = "/api/hermandades/" + HID + "/procesiones/" + PID + "/pasos/" + PASO_ID + "/cruceta";
        assertThat(firstMatchingRouteId(base)).isEqualTo("repertorio-cruceta");
        assertThat(firstMatchingRouteId(base + "/run-sheet")).isEqualTo("repertorio-cruceta");
        assertThat(firstMatchingRouteId(base + "/current")).isEqualTo("repertorio-cruceta");
    }

    @Test
    void remainingPathsKeepExistingRouting() {
        assertThat(firstMatchingRouteId("/api/hermandades/" + HID + "/titulares"))
            .isEqualTo("hermandad-service");
        assertThat(firstMatchingRouteId("/api/hermandades/" + HID))
            .isEqualTo("hermandad-service");
        assertThat(firstMatchingRouteId("/api/marchas")).isEqualTo("repertorio-service");
        assertThat(firstMatchingRouteId("/api/procesiones/" + PID)).isEqualTo("procesion-service");
    }

    @Test
    void specificRoutesPrecedeTheHermandadCatchAll() {
        List<String> ids = routeLocator.getRoutes().collectList().block().stream()
            .map(Route::getId)
            .toList();
        assertThat(ids).containsSubsequence(
            "repertorio-cruceta",
            "procesion-pasos",
            "procesion-route",
            "procesion-plan",
            "hermandad-service");
    }
}
