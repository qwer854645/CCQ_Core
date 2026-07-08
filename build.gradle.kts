import java.security.MessageDigest

plugins {
    java
    id("net.neoforged.moddev") version "1.0.21"
}

version = project.property("mod_version") as String

base {
    archivesName.set("ccq_core")
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))

configurations {
    create("localRuntime")
    named("runtimeClasspath") {
        extendsFrom(configurations["localRuntime"])
    }
}

neoForge {
    version = project.property("neo_version") as String

    parchment {
        minecraftVersion = project.property("minecraft_version") as String
        mappingsVersion = "2024.11.17"
    }

    runs {
        register("client") {
            client()
        }
    }

    mods {
        register("ccq_core") {
            sourceSet(sourceSets.main.get())
        }
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

    val coeJar = file("libs/createoreexcavation-1.21-1.6.8.jar")
    if (coeJar.exists()) {
        compileOnly(files(coeJar))
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

    val devModpackModsDir = providers.gradleProperty("dev_modpack_mods_dir").map { file(it) }
    val devRuntimeMods = listOf(
        "create-1.21.1-6.0.10.jar",
        "create-aeronautics-bundled-1.21.1-1.3.0.jar",
        "sable-neoforge-1.21.1-2.0.3.jar",
        "createoreexcavation-1.21-1.6.8.jar",
        "jei-1.21.1-neoforge-19.27.0.343.jar",
        "Create More Recipes-1.21.1-1.2.1-fix1.jar",
        "createaddition-1.6.0.jar",
        "Jade-1.21.1-NeoForge-15.10.5.jar",
        "tacz-neoforge-1.21.1-1.1.8-hotfix-r2.jar",
    )
    if (devModpackModsDir.isPresent && devModpackModsDir.get().exists()) {
        val modsDir = devModpackModsDir.get()
        val runtimeJars = devRuntimeMods.mapNotNull { name ->
            val candidate = file("$modsDir/$name")
            if (candidate.exists()) candidate else {
                logger.warn("Dev runtime mod not found: $name")
                null
            }
        }
        if (runtimeJars.isNotEmpty()) {
            add("localRuntime", files(runtimeJars))
            logger.lifecycle("Dev client will load ${runtimeJars.size} runtime mods from ${modsDir.absolutePath}")
        }
    } else {
        logger.warn("dev_modpack_mods_dir not found; runClient will only load ccq_core.")
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.named<ProcessResources>("processResources") {
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(mapOf("mod_version" to project.version))
    }
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
