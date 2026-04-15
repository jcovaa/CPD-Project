plugins {
    id("java")
    id("application")
}

application {
    mainClass = "pt.up.fe.t06g10.Main"
}

group = "pt.up.fe.t06g10"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}