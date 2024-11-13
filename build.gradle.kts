import dev.extframework.gradle.common.extFramework

plugins {
    kotlin("jvm") version "2.0.0"

    id("dev.extframework.mc") version "1.2.20" apply false
    id("dev.extframework.common") version "1.0.36" apply false
}

allprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "dev.extframework.mc")
    apply(plugin = "dev.extframework.common")

    group = "dev.extframework.extension"
    version = "1.0-BETA"

    repositories {
        mavenLocal()
        mavenCentral()
        extFramework()
        maven {
            url = uri("https://repo.extframework.dev/registry")
        }
    }

    kotlin {
        jvmToolchain(8)
    }
}