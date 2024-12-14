import com.undefinedcreation.runServer.ServerType

plugins {
    java
    kotlin("jvm") version "1.9.22"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.undefinedcreation.runServer") version "0.0.1"
    id("io.papermc.paperweight.userdev") version "1.7.1"
}

group = "com.undefined"
version = "0.0.1"

val minecraftVersion = "1.21.3"

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.REOBF_PRODUCTION

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "undefinedRepo"
        url = uri("https://repo.undefinedcreation.com/repo")
    }
    maven {
        name = "undefinedRepo"
        url = uri("https://repo.undefinedcreation.com/releases")
    }
    maven("https://repo.codemc.io/repository/maven-snapshots/")
    maven { url = uri("https://maven.enginehub.org/repo/") }
    maven {
        name = "henkelmax.public"
        url = uri("https://maven.maxhenkel.de/repository/public")
        content {
            includeGroup("de.maxhenkel.voicechat")
        }
    }
    maven {
        url = uri("https://repo.inventivetalent.org/repository/public/")
    }
}


dependencies {
    paperweight.paperDevBundle("1.21.3-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    implementation("com.undefined:api:0.5.94:mapped")
    implementation("com.undefined:akari:0.0.6:mapped")
    implementation("com.undefined:stellar:0.0.8")

    implementation("de.maxhenkel.voicechat:voicechat-api:2.5.0")

    // World EDit
    implementation(platform("com.intellectualsites.bom:bom-newest:1.51")) // Ref: https://github.com/IntellectualSites/bom
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit") { isTransitive = false }
}

tasks {
    assemble {
        dependsOn(shadowJar)
    }

    shadowJar {
        archiveFileName.set("${this.project.name}-shadow.jar")
    }

    compileKotlin {
        kotlinOptions.jvmTarget = "21"
    }

    runServer {
        mcVersion(minecraftVersion)
        serverType(ServerType.PAPERMC)
        acceptMojangEula(true)
    }
}

//kotlin {
//    jvmToolchain(21)
//}