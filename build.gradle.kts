plugins {
    java
    id("org.springframework.boot") version "3.5.9"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "7.0.2"
    id("org.flywaydb.flyway") version "12.0.1"
}

group = "camp.nextstep.edu"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")
    implementation(platform("io.jsonwebtoken:jjwt-bom:0.13.0"))
    implementation("io.jsonwebtoken:jjwt-api")
    runtimeOnly("io.jsonwebtoken:jjwt-impl")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson")
    runtimeOnly("com.h2database:h2")
    runtimeOnly("com.mysql:mysql-connector-j")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.rest-assured:rest-assured:5.5.1")
    testImplementation("io.cucumber:cucumber-java:7.21.1")
    testImplementation("io.cucumber:cucumber-spring:7.21.1")
    testImplementation("io.cucumber:cucumber-junit-platform-engine:7.21.1")
    testImplementation("org.junit.platform:junit-platform-suite")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

spotless {
    java {
        target("src/*/java/**/*.java")
        palantirJavaFormat()
        removeUnusedImports()
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeEngines("cucumber")
    }
    exclude("**/cucumber/**")
}

tasks.register<Test>("cucumberTest") {
    description = "Runs Cucumber acceptance tests"
    group = "verification"
    dependsOn("dockerUp")
    finalizedBy("dockerDown")
    useJUnitPlatform()
    include("**/RunCucumberTest.class")
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

tasks.register<Exec>("dockerBuild") {
    group = "docker"
    description = "Builds the Docker image for the application"
    commandLine("docker", "build", "-t", "gift-app", ".")
}

tasks.register<Exec>("dockerUp") {
    group = "docker"
    description = "Starts the application and database containers"
    commandLine("docker-compose", "up", "-d", "--build", "--wait")
}

tasks.register<Exec>("dockerDown") {
    group = "docker"
    description = "Stops and removes the application and database containers"
    commandLine("docker-compose", "down", "-v")
}
