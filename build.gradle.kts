plugins {
    kotlin("jvm") version "2.1.0"
    id("maven-publish")
}

group = "com.kvxd"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    implementation(kotlin("reflect"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = this.groupId
            artifactId = "keventbus"
            version = this.version
        }
    }
}