plugins {
    kotlin("jvm") version "1.9.21"
}

group = "de.p10r"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.mongodb:mongodb-driver-kotlin-sync:4.11.0")
    implementation("org.slf4j:slf4j-api:2.0.10")


    testImplementation(kotlin("test"))
    testImplementation("org.testcontainers:testcontainers:1.19.3")
    testImplementation("org.testcontainers:mongodb:1.19.3")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}