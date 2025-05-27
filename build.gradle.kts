import dev.extframework.gradle.common.extFramework

plugins {
    kotlin("jvm") version "2.0.0"

    id("dev.extframework") version "1.3.2" apply false
    id("dev.extframework.common") version "1.0.53" apply false
}

repositories {
    mavenCentral()
}

task("publishExtensions") {
    dependsOn(":mappings:publishExtension")
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "dev.extframework")
    apply(plugin = "dev.extframework.common")

    group = "dev.extframework.extension"

    repositories {
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