package com.repertorio.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:9999/realms/test",
    "eureka.client.enabled=false"
})
class ApiGatewayApplicationTests {

    @Test
    void contextLoads() {
    }
}
