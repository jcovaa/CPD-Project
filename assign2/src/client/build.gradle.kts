plugins {
    id("java")
    id("application")
}

dependencies {
    implementation(project(":shared"))
}

application {
    mainClass.set("pt.up.fe.t06g10.client.ChatClient")
}
