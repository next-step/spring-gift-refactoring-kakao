plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    kotlin("plugin.jpa") version "1.9.25"
    id("org.springframework.boot") version "3.5.9"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
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
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")
    implementation(platform("io.jsonwebtoken:jjwt-bom:0.13.0"))
    implementation("io.jsonwebtoken:jjwt-api")
    runtimeOnly("io.jsonwebtoken:jjwt-impl")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson")
    runtimeOnly("com.h2database:h2")
    runtimeOnly("com.mysql:mysql-connector-j")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("io.rest-assured:rest-assured:5.4.0")
    testImplementation("io.cucumber:cucumber-java:7.18.1")
    testImplementation("io.cucumber:cucumber-spring:7.18.1")
    testImplementation("io.cucumber:cucumber-junit-platform-engine:7.18.1")
    testImplementation("org.junit.platform:junit-platform-suite")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

ktlint {
    verbose.set(true)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.register<Exec>("dockerBuild") {
    commandLine("/opt/homebrew/bin/docker-compose", "-f", "docker-compose.e2e.yml", "build")
}

tasks.register<Exec>("dockerUp") {
    commandLine("/opt/homebrew/bin/docker-compose", "-f", "docker-compose.e2e.yml", "up", "-d", "--wait")
}

tasks.register<Exec>("dockerDown") {
    commandLine("/opt/homebrew/bin/docker-compose", "-f", "docker-compose.e2e.yml", "down", "--rmi", "local")
}

tasks.register<Test>("cucumberTest") {
    useJUnitPlatform()
    outputs.upToDateWhen { false }
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
    systemProperty("cucumber.glue", "gift.e2e")
    systemProperty("cucumber.features", "src/test/resources/features")
    systemProperty("cucumber.plugin", "pretty")
    include("gift/e2e/**")
}

tasks.register("cucumberE2eTest") {
    dependsOn("dockerUp", "cucumberTest")
    finalizedBy("dockerDown")
}

tasks.named("cucumberTest") {
    mustRunAfter("dockerUp")
}
