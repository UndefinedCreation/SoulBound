plugins {
    java
    kotlin("jvm") version "1.9.22"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.undefinedcreation.runServer") version "0.0.1"
}

group = "com.undefined"
version = "0.0.1"

val minecraftVersion = "1.21.3"

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
    maven("https://repo.codemc.io/repository/maven-snapshots/")
}


dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.3-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    implementation("com.undefined:api:0.5.94:mapped")
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

        acceptMojangEula(true)
    }
}

kotlin {
    jvmToolchain(21)
}