plugins {
    alias(libs.plugins.spring.boot)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

dependencies {
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.webflux)
    // ponytail: 5.0.2 (server-webflux variant) targets Boot 4.x; 4.3.5 (standard starter) references NettyWebServerFactoryCustomizer removed in Boot 4
    implementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webflux:5.0.2")
    implementation(libs.spring.cloud.starter.netflix.eureka.client)
    implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")
    implementation(libs.spring.boot.starter.security)
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation(libs.springdoc.starter.webflux)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webflux.test)
}
