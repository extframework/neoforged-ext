import com.kaolinmc.gradle.common.kaolin

plugins {
    kotlin("jvm") version "2.0.0"

    id("kaolin.kiln") version "0.1.6" apply false
    id("com.kaolinmc.common") version "0.1.6" apply false
}

repositories {
    mavenCentral()
}

task("publishExtensions") {
    dependsOn(":mappings:publishExtension")
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "kaolin.kiln")
    apply(plugin = "com.kaolinmc.common")

    group = "com.kaolinmc.extension"

    repositories {
        mavenLocal()
        mavenCentral()
        kaolin()
    }

    kotlin {
        jvmToolchain(8)
    }
}