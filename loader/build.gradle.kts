import com.kaolinmc.core.main.main
import com.kaolinmc.gradle.common.artifactResolverMaven
import com.kaolinmc.kiln.api.EvaluatingDependency
import com.kaolinmc.kiln.publish.ExtensionPublication
import com.kaolinmc.minecraft.MojangNamespaces
import com.kaolinmc.minecraft.task.LaunchMinecraft
import com.kaolinmc.tooling.api.extension.ExtensionRepository

version = "1.0-BETA"
group = "com.kaolinmc.integration"

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.neoforged.net/releases")
    }
    maven {
        url = uri("https://libraries.minecraft.net")
    }
}

val launch1_21_4 by tasks.registering(LaunchMinecraft::class) {
    dependsOn(tasks.named("publishToMavenLocal"))
    targetNamespace = MojangNamespaces.deobfuscated.identifier
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(21))
    })
    mcVersion = "1.21.7"

    jvmArgs(
        "--add-opens",
        "java.base/java.nio.file.spi=ALL-UNNAMED",
        "-Dlog4j2.configurationFile=/Users/durganmcbroom/IdeaProjects/FancyModLoader/tests/build/moddev/clientDataLog4j2.xml"
//        "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"
    )
}

extension {
    finalizedBy {
        model {
            partition("main") {
                dependencies.addAll(
                    EvaluatingDependency.Raw(
                        mapOf(
                            "neoforge-version" to "21.7.23-beta",
                        )
                    ),
                )

                repositories.addAll(
                    ExtensionRepository(
                        "neoforge",
                        mutableMapOf()
                    ),
                )
            }
        }
    }
    model {
        name = "neoforge"
    }
    partitions {
        main {
            extensionClass = "com.kaolinmc.integration.neoforge.NeoforgeIntegration"
            dependencies {
//                addDependency(EvaluatingDependency.Raw(mapOf(
//                   "descriptor" to "net.neoforged:neoforge:21.7.25-beta:installer",
//                    "isTransitive" to "false"
//                )))
            }
        }
        tweaker {
            tweakerClass = "com.kaolinmc.integration.neoforge.NeoForgeTweaker"
            dependencies {
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.19.0")
                implementation(artifactResolverMaven())
            }
        }
    }

    metadata {
        name = "NeoForged"
        description = "the Neo FML implemented in Kaolin"
        developers.add("kaolin")
        app = "minecraft"
    }
}

dependencies {
    "tweakerImplementation"("net.neoforged.fancymodloader:loader:9.0.14")
    "tweakerImplementation"("net.neoforged.fancymodloader:securejarhandler:9.0.14")

    implementation("net.neoforged.fancymodloader:securejarhandler:9.0.14")
    implementation("net.fabricmc:sponge-mixin:0.14.0+mixin.0.8.6")
    implementation("net.neoforged:JarJarFileSystems:0.4.1")
    implementation("net.neoforged.fancymodloader:loader:9.0.14")
    implementation("net.neoforged.fancymodloader:bootstraplauncher:9.0.14")
//    implementation("net.neoforged:neoforge:21.7.25-beta:installer")
}

kotlin {
    jvmToolchain(21)
}

publishing {
    publications {
        create("prod", ExtensionPublication::class.java)
    }
    repositories {
        maven {
            url = uri("https://repo.kaolinmc.com")
            credentials {
                password = properties["creds.ext.key"] as? String
            }
        }
    }
}

tasks.named<org.gradle.jvm.tasks.Jar>("tweakerJar") {
    manifest {
        attributes("Automatic-Module-Name" to "kaolinmc.integration.forge.tweaker")
    }
    from(project("null-launch-target").tasks.getByName("jar"))
}
