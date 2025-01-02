import dev.extframework.gradle.common.archiveMapper
import dev.extframework.gradle.common.commonUtil
import dev.extframework.gradle.common.coreApi
import dev.extframework.gradle.common.dm.jobs
import dev.extframework.gradle.common.toolingApi
import dev.extframework.gradle.publish.ExtensionPublication

version = "1.0.2-BETA"

tasks.launch {
    mcVersion = "1.8.9"
    targetNamespace = "mcp-legacy:deobfuscated"
}

extension {
    model {
        name = "mcp-mappings"
    }
    partitions {
        main {
            extensionClass = "dev.extframework.extension.neoforge.mapping.NeoforgeMappingExtension"
            dependencies {
                coreApi()
            }
        }

        tweaker {
            tweakerClass = "dev.extframework.extension.neoforge.mapping.NeoforgeMappingTweaker"
            dependencies {
                toolingApi()
                jobs()
                archiveMapper(mcpLegacy = true)
                commonUtil()
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.0")
            }
        }
    }

    metadata {
        name = "Neoforged Mappings"
        description = "An extension providing MCP (legacy) mappings for Neoforged"
        developers.add("extframework")
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