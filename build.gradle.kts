import java.security.MessageDigest

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

    val capgVersion = property("capg_version") as String
    val capgJar = file("libs/createaerophysicsgantry-$capgVersion.jar")
    if (capgJar.exists()) {
        "additionalRuntimeClasspath"(files(capgJar))
    } else {
        logger.warn("Missing ${capgJar.name}; ccq_core will not embed Create Aeronautics Physics Gantry.")
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

val capgVersion = providers.gradleProperty("capg_version")
val capgJarFile = capgVersion.map { file("libs/createaerophysicsgantry-$it.jar") }

val generateCapgJarJarMetadata = tasks.register("generateCapgJarJarMetadata") {
    val capgJar = capgJarFile.get()
    onlyIf { capgJar.exists() }

    val metadataDir = layout.buildDirectory.dir("generated/capg-jarjar/META-INF/jarjar")
    outputs.dir(metadataDir)

    doLast {
        val digest = MessageDigest.getInstance("MD5").digest(capgJar.readBytes())
        val md5Version = digest.joinToString("") { "%02x".format(it) }
        val embeddedName = capgJar.name
        val metadata = """
            {
              "jars": [
                {
                  "identifier": {
                    "group": "",
                    "artifact": "${embeddedName.removeSuffix(".jar")}"
                  },
                  "version": {
                    "range": "[$md5Version,)",
                    "artifactVersion": "$md5Version"
                  },
                  "path": "META-INF/jarjar/$embeddedName",
                  "isObfuscated": false
                }
              ]
            }
        """.trimIndent()

        val outDir = metadataDir.get().asFile
        outDir.mkdirs()
        File(outDir, "metadata.json").writeText(metadata)
        capgJar.copyTo(File(outDir, embeddedName), overwrite = true)
    }
}

tasks.named<Jar>("jar") {
    if (capgJarFile.get().exists()) {
        dependsOn(generateCapgJarJarMetadata)
        from(layout.buildDirectory.dir("generated/capg-jarjar"))
    }
}
