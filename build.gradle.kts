plugins {
    id("java")
}

group = "dev.rickysixx"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("info.picocli:picocli:4.7.4")
}

tasks.test {
    useJUnitPlatform()
}