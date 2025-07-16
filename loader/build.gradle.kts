import com.kaolinmc.core.main.main
import com.kaolinmc.gradle.common.artifactResolverMaven
import com.kaolinmc.kiln.api.EvaluatingDependency
import com.kaolinmc.kiln.publish.ExtensionPublication
import com.kaolinmc.minecraft.MojangNamespaces
import com.kaolinmc.minecraft.task.LaunchMinecraft
import com.kaolinmc.tooling.api.extension.ExtensionRepository
import org.gradle.kotlin.dsl.assign

version = "1.0-BETA"
group = "com.kaolinmc.integration"

val launch1_21_4 by tasks.registering(LaunchMinecraft::class) {
    dependsOn(tasks.named("publishToMavenLocal"))
    targetNamespace = MojangNamespaces.deobfuscated.identifier
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(21))
    })
    mcVersion = "1.21.4"
}

extension {
    finalizedBy {
        model {
            partition("main") {
                dependencies.addAll(
                    EvaluatingDependency.Raw(
                        mapOf(
                            "neoforge-version" to "21.7.23-beta"
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