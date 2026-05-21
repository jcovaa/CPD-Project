rootProject.name = "DistributedChat"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

include("server")
include("client")
