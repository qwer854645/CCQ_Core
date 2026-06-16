plugins {
    java
    id("net.neoforged.moddev") version "1.0.21"
}

base {
    archivesName.set("ccq_core")
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))

neoForge {
    version = project.property("neo_version") as String

    parchment {
        minecraftVersion = project.property("minecraft_version") as String
        mappingsVersion = "2024.11.17"
    }
}

repositories {
    mavenCentral()
    maven("https://maven.neoforged.net/releases")
    maven("https://repo.maven.appliedenergistics.org")
}

dependencies {
    compileOnly("org.appliedenergistics:appliedenergistics2:${property("ae2_version")}")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
