import dev.extframework.gradle.common.archiveMapper
import dev.extframework.gradle.common.archiveMapperMcpLegacy
import dev.extframework.gradle.publish.ExtensionPublication

version = "1.0.6-BETA"

extension {
    model {
        name = "mcp-mappings"
    }
    partitions {
        tweaker {
            tweakerClass = "dev.extframework.extension.neoforge.mapping.NeoforgeMappingTweaker"
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
        developers.add("extframework")
        app = "minecraft"
    }
}

publishing {
    publications {
        create("prod", ExtensionPublication::class.java)
    }
    repositories {
        maven {
            url = uri("https://repo.extframework.dev")
            credentials {
                password = properties["creds.ext.key"] as? String
            }
        }
    }
}