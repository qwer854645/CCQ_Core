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
    maven("https://maven.createmod.net")
    maven("https://mvn.devos.one/snapshots")
    maven("https://maven.iglee.fr/releases")
    maven("https://maven.blamejared.com")
    maven("https://api.modrinth.com/maven")
}

dependencies {
    val createSlim = file("libs/create-1.21.1-6.0.10-280-slim.jar")
    if (createSlim.exists()) {
        compileOnly(files(createSlim))
    } else {
        compileOnly("com.simibubi.create:create-${property("minecraft_version")}:${property("create_version")}:slim") {
            isTransitive = false
        }
    }

    compileOnly("net.createmod.ponder:ponder-neoforge:${property("ponder_version")}+mc${property("minecraft_version")}") {
        isTransitive = false
    }

    compileOnly("dev.engine-room.flywheel:flywheel-neoforge-api-${property("minecraft_version")}:${property("flywheel_version")}") {
        isTransitive = false
    }

    compileOnly("fr.iglee42:CreateMoreRecipes:${property("cmr_version")}") {
        isTransitive = false
    }

    val jeiJar = file("libs/jei-1.21.1-neoforge-api.jar")
    if (jeiJar.exists()) {
        compileOnly(files(jeiJar))
    } else {
        compileOnly("mezz.jei:jei-${property("minecraft_version")}-neoforge-api:${property("jei_version")}") {
            isTransitive = false
        }
    }

    val jadeJar = file("libs/jade-1.21.1-neoforge-api.jar")
    if (jadeJar.exists()) {
        compileOnly(files(jadeJar))
    }

    testImplementation("com.google.code.gson:gson:2.11.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.slf4j:slf4j-simple:2.0.16")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
