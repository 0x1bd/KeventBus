plugins {
    kotlin("jvm") version "2.0.21"
    id("maven-publish")
}

group = "com.github.meo209"
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
            groupId = "com.github.meo209"
            artifactId = "keventbus" // Replace with your desired artifact ID
            version = "1.0-SNAPSHOT"
        }
    }
}