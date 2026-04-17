plugins {
    id("java")
}

group = "pt.up.fe.t06g10"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

subprojects {
    apply(plugin = "java")
}
