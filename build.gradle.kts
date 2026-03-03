plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    kotlin("plugin.jpa") version "1.9.25"
    id("org.springframework.boot") version "3.5.9"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
    id("org.flywaydb.flyway") version "12.0.1"
    id("com.diffplug.spotless") version "7.0.2"
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
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("io.cucumber:cucumber-java:7.22.1")
    testImplementation("io.cucumber:cucumber-spring:7.22.1")
    testImplementation("io.cucumber:cucumber-junit-platform-engine:7.22.1")
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

spotless {
    java {
        target("src/**/*.java")
        googleJavaFormat()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.named<Test>("test") {
    exclude("gift/cucumber/**")
}

// Docker 소켓 자동 탐색: DOCKER_HOST > 런타임별 소켓 경로 > /var/run/docker.sock
fun detectDockerHost(): String? {
    val dockerHost = System.getenv("DOCKER_HOST")
    if (!dockerHost.isNullOrBlank()) return dockerHost

    val home = System.getProperty("user.home")
    val candidates =
        listOf(
            "$home/.docker/run/docker.sock", // Docker Desktop (macOS)
            "$home/.docker/desktop/docker.sock", // Docker Desktop (Linux)
            "$home/.colima/default/docker.sock", // Colima
            "$home/.orbstack/run/docker.sock", // OrbStack
            "$home/.rd/docker.sock", // Rancher Desktop
            "/var/run/docker.sock", // Linux native / symlink
        )
    val found = candidates.firstOrNull { File(it).exists() }
    return found?.let { "unix://$it" }
}

fun dockerHostEnv(): Map<String, String> {
    val dockerHost = detectDockerHost() ?: return emptyMap()
    return mapOf("DOCKER_HOST" to dockerHost)
}

tasks.register<Exec>("dockerBuild") {
    group = "docker"
    description = "Docker 이미지 빌드"
    commandLine("docker", "build", "-t", "spring-gift-test:latest", ".")
    environment(dockerHostEnv())
}

tasks.register<Exec>("dockerUp") {
    group = "docker"
    description = "Docker Compose 시작"
    commandLine("docker", "compose", "up", "-d", "--wait")
    environment(dockerHostEnv())
}

tasks.register<Exec>("dockerDown") {
    group = "docker"
    description = "Docker Compose 종료"
    commandLine("docker", "compose", "down")
    environment(dockerHostEnv())
}

tasks.register<Test>("cucumberTest") {
    useJUnitPlatform()
    include("gift/cucumber/CucumberTest.class")
    group = "verification"
    description = "Cucumber 인수 테스트 (Docker PostgreSQL)"
}
