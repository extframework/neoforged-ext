import com.kaolinmc.gradle.common.boot
import org.gradle.kotlin.dsl.project

plugins {
    kotlin("jvm")
}

group = "dev.extframework.integrations"

repositories {
    maven {
        url = uri("https://maven.neoforged.net/releases")
    }
    maven {
        url = uri("https://libraries.minecraft.net")
    }
    mavenCentral()
}

dependencies {
    implementation("net.neoforged.fancymodloader:securejarhandler:9.0.14")
    implementation("net.neoforged.fancymodloader:loader:9.0.14")
    implementation("net.neoforged.fancymodloader:bootstraplauncher:9.0.14")
}

kotlin {
    jvmToolchain(21)
}

tasks.compileJava {
    destinationDirectory.set(tasks.compileKotlin.get().destinationDirectory.get())
//    options.compilerArgs.addAll(listOf(
//        "--add-reads", "kaolinmc.forge.launch=ALL-UNNAMED"
//    ))
}

//tasks.compileKotlin {
//    compilerOptions.freeCompilerArgs.addAll(listOf(
//        "-Xadd-reads", "kaolinmc.forge.launch=ALL-UNNAMED"
//    ))
//}