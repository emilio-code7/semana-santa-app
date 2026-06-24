plugins {
    alias(libs.plugins.spring.boot)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

dependencies {
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.webflux)
    implementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webflux")
    implementation(libs.spring.cloud.starter.netflix.eureka.client)
    implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")
    implementation(libs.spring.boot.starter.security)
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webflux.test)
}
