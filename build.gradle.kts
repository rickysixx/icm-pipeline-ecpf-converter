plugins {
    id("java")
}

group = "dev.rickysixx"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

tasks.test {
    useJUnitPlatform()
}