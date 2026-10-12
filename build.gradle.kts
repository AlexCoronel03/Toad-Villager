plugins {
    id("fabric-loom") version "1.17.16"
    `maven-publish`
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

base {
    archivesName.set(project.property("archives_base_name") as String)
}

loom {
    runs {
        named("client") {
            client()
            configName = "Toad Client"
            runDir("run/client")
            // Dev-only visual verification: ./gradlew runClient -PtoadPreview
            if (project.hasProperty("toadPreview")) {
                vmArg("-Dtoadmod.preview=true")
                vmArg("-Dtoadmod.previewMode=" + project.property("toadPreview"))
            }
        }
        named("server") {
            server()
            configName = "Toad Dedicated Server"
            runDir("run/server")
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_version")}")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to project.version))
    }
}
