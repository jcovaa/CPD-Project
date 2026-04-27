plugins {
    id("java")
    id("application")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":shared"))
    implementation("com.googlecode.lanterna:lanterna:3.1.1")
}

application {
    mainClass.set("pt.up.fe.t06g10.client.Main")
}

tasks.withType<JavaExec> {
    standardInput = System.`in`
}