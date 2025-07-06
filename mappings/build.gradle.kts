import com.kaolinmc.gradle.common.archiveMapper
import com.kaolinmc.gradle.common.archiveMapperMcpLegacy
import com.kaolinmc.kiln.publish.ExtensionPublication

version = "1.0.7-BETA"

extension {
    model {
        name = "mcp-mappings"
    }
    partitions {
        tweaker {
            tweakerClass = "com.kaolinmc.extension.neoforge.mapping.NeoforgeMappingTweaker"
            dependencies {
                implementation(archiveMapper())
                implementation(archiveMapperMcpLegacy())
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.19.0")
            }
        }
    }

    metadata {
        name = "Neoforged Mappings"
        description = "An extension providing MCP (legacy) mappings for Neoforged"
        developers.add("kaolin")
        app = "minecraft"
    }
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