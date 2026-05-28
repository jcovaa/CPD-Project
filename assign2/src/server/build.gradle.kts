plugins {
    id("java")
    id("application")
    id("com.gradleup.shadow") version "9.4.1"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.hibernate.orm:hibernate-core:6.5.2.Final")
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("io.github.cdimascio:dotenv-java:3.2.0")
}

application {
    mainClass.set("pt.up.fe.t06g10.server.Main")
}

tasks.shadowJar {
    archiveBaseName.set("server")
    archiveVersion.set("")
    archiveClassifier.set("")
}
