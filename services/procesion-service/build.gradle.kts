plugins {
    alias(libs.plugins.spring.boot)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

dependencies {
    implementation(project(":shared:common"))

    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.security)
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.cloud.starter.netflix.eureka.client)
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-kafka")

    implementation(libs.spring.boot.starter.flyway)
    runtimeOnly("org.flywaydb:flyway-database-postgresql")

    runtimeOnly(libs.postgresql)
    implementation(libs.spring.boot.starter.jackson)

    implementation(libs.springdoc.starter)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.security.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.starter.data.jpa.test)
    testRuntimeOnly(libs.h2database)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgresql)
}
