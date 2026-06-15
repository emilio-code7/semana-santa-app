plugins {
    alias(libs.plugins.spring.boot)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

dependencies {
    implementation(libs.spring.cloud.starter.netflix.eureka.server)
    testImplementation(libs.spring.boot.starter.test)
}
