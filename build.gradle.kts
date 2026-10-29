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

    val jadeFullJar = file("libs/Jade-1.21.1-NeoForge-15.10.6.jar")
    val jadeApiJar = file("libs/jade-1.21.1-neoforge-api.jar")
    if (jadeFullJar.exists()) {
        compileOnly(files(jadeFullJar))
    } else if (jadeApiJar.exists()) {
        compileOnly(files(jadeApiJar))
    }

    val coeJar = file("libs/createoreexcavation-1.21-1.6.8.jar")
    if (coeJar.exists()) {
        compileOnly(files(coeJar))
    }

    val fxntJar = file("libs/fxntstorage-1.3.2.jar")
    if (fxntJar.exists()) {
        compileOnly(files(fxntJar))
    } else {
        logger.warn("Missing ${fxntJar.name}; storage bridge will not compile against FXNT Storage.")
    }

    val functionalStorageJar = file("libs/functionalstorage-1.21.1-1.5.8.jar")
    if (functionalStorageJar.exists()) {
        compileOnly(files(functionalStorageJar))
    } else {
        logger.warn("Missing ${functionalStorageJar.name}; storage bridge will not compile against Functional Storage.")
    }

    val titaniumJar = file("libs/titanium-1.21-4.0.43.jar")
    if (titaniumJar.exists()) {
        compileOnly(files(titaniumJar))
    }

    val fluidJar = file("libs/fluid-1.2.4.jar")
    if (fluidJar.exists()) {
        compileOnly(files(fluidJar))
    } else {
        logger.warn("Missing ${fluidJar.name}; Create Fluid interface survive mixins will not compile.")
    }

    val capgVersion = property("capg_version") as String
    val capgJar = file("libs/createaerophysicsgantry-$capgVersion.jar")
    if (capgJar.exists()) {
        // Loaded through generated jar-in-jar resources, including in development runs.
    } else {
        logger.warn("Missing ${capgJar.name}; ccq_core will not embed Create Aeronautics Physics Gantry.")
    }

    val bnbCogVersion = property("bnb_cogwheel_compat_version") as String
    val bnbCogJar = file("libs/bnb_cogwheel_compat-$bnbCogVersion.jar")
    if (bnbCogJar.exists()) {
        // Do not also put embedded mods on the Java classpath (duplicate module packages).
    } else {
        logger.warn("Missing ${bnbCogJar.name}; ccq_core will not embed BnB Cogwheel Compat.")
    }

    testImplementation("com.google.code.gson:gson:2.11.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.slf4j:slf4j-simple:2.0.16")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    val devModpackModsDir = providers.gradleProperty("dev_modpack_mods_dir").map { file(it) }
    val devRuntimeMods = listOf(
        "create-1.21.1-6.0.10.jar",
        "create-aeronautics-bundled-1.21.1-1.3.2.jar",
        "sable-neoforge-1.21.1-2.0.6.jar",
        "createoreexcavation-1.21-1.6.8.jar",
        "jei-1.21.1-neoforge-19.57.0.451.jar",
        "Create More Recipes-1.21.1-1.2.1-fix1.jar",
        "createaddition-1.7.2.jar",
        "Jade-1.21.1-NeoForge-15.10.6.jar",
        "tacz-neoforge-1.21.1-1.1.8-hotfix-r7.jar",
        "fxntstorage-1.3.8+mc-1.21.1-neoforge.jar",
        "functionalstorage-1.21.1-1.5.8.jar",
        "titanium-1.21-4.0.43.jar",
        "FarmersDelight-1.21.1-1.3.4.jar",
        "FarmersRespite-1.21.1-3.0.1.jar",
        "spartan_weaponry_unofficial-1.2.3-1.21.1.jar",
        "fluid-1.2.4.jar",
        "architectury-13.0.11-neoforge.jar",
        "curios-neoforge-9.5.1+1.21.1.jar",
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

// Storage handler tests need the same Minecraft / NeoForge API as production sources.
sourceSets.test {
    compileClasspath += sourceSets.main.get().compileClasspath
    runtimeClasspath += sourceSets.main.get().compileClasspath
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.named<ProcessResources>("processResources") {
    // Ensure mods.toml re-expands when only gradle.properties version changes.
    inputs.property("mod_version", project.version)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(mapOf("mod_version" to project.version))
    }
}

val capgVersion = providers.gradleProperty("capg_version")
val capgJarFile = capgVersion.map { file("libs/createaerophysicsgantry-$it.jar") }
val bnbCogVersion = providers.gradleProperty("bnb_cogwheel_compat_version")
val bnbCogJarFile = bnbCogVersion.map { file("libs/bnb_cogwheel_compat-$it.jar") }

fun jarJarEntryJson(jar: File): String {
    val digest = MessageDigest.getInstance("MD5").digest(jar.readBytes())
    val md5Version = digest.joinToString("") { "%02x".format(it) }
    val embeddedName = jar.name
    val artifact = embeddedName.removeSuffix(".jar")
    return """
                {
                  "identifier": {
                    "group": "",
                    "artifact": "$artifact"
                  },
                  "version": {
                    "range": "[$md5Version,)",
                    "artifactVersion": "$md5Version"
                  },
                  "path": "META-INF/jarjar/$embeddedName",
                  "isObfuscated": false
                }""".trimIndent()
}

val generateJarJarMetadata = tasks.register("generateJarJarMetadata") {
    val jarsProvider = provider {
        buildList {
            val capg = capgJarFile.get()
            if (capg.exists()) add(capg)
            val bnb = bnbCogJarFile.get()
            if (bnb.exists()) add(bnb)
        }
    }
    inputs.files(jarsProvider)
    val metadataDir = layout.buildDirectory.dir("generated/ccq-jarjar/META-INF/jarjar")
    outputs.dir(metadataDir)
    onlyIf { jarsProvider.get().isNotEmpty() }

    doLast {
        val jars = jarsProvider.get()
        val outDir = metadataDir.get().asFile
        if (outDir.exists()) {
            outDir.listFiles()?.forEach { it.delete() }
        } else {
            outDir.mkdirs()
        }
        val metadata = buildString {
            appendLine("{")
            appendLine("  \"jars\": [")
            jars.forEachIndexed { index, jar ->
                append(jarJarEntryJson(jar))
                if (index < jars.lastIndex) append(",")
                appendLine()
            }
            appendLine("  ]")
            appendLine("}")
        }
        File(outDir, "metadata.json").writeText(metadata)
        jars.forEach { it.copyTo(File(outDir, it.name), overwrite = true) }
        logger.lifecycle("Jar-in-Jar: embedded ${jars.joinToString { it.name }}")
    }
}

afterEvaluate {
    tasks.named<ProcessResources>("processResources").configure {
        dependsOn(generateJarJarMetadata)
        from(layout.buildDirectory.dir("generated/ccq-jarjar"))
    }
    tasks.named<Jar>("jar").configure {
        dependsOn(generateJarJarMetadata)
    }
}
