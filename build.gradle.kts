plugins {
    id("java")
    id("com.intershop.gradle.jaxb") version "6.0.0"
}

group = "dev.rickysixx"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // JAXB dependencies
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.0")
    implementation("org.glassfish.jaxb:jaxb-runtime:4.0.3")

    implementation("org.jgrapht:jgrapht-core:1.5.2")

    implementation("info.picocli:picocli:4.7.4")
}

tasks.test {
    useJUnitPlatform()
}

jaxb {
    javaGen {
        register("pipeline") {
            schema = file("xsd/PipelineXML.xsd")
            binding = file("xjc/bindings.xjb")
            extension = true
        }
    }
}