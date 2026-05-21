plugins {
    id("java")
    id("application")
    id("com.gradleup.shadow") version "9.4.1"
}

application {
    mainClass.set("pt.up.fe.t06g10.client.Main")
}

tasks.shadowJar {
    archiveBaseName.set("client")
    archiveVersion.set("")
    archiveClassifier.set("")
}
