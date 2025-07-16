package com.kaolinmc.integration.neoforge.artifact

import com.durganmcbroom.artifact.resolver.ArtifactRepository
import com.durganmcbroom.artifact.resolver.MetadataRequestException
import com.durganmcbroom.artifact.resolver.RepositoryFactory
import com.durganmcbroom.artifact.resolver.simple.maven.SimpleMavenRepositorySettings
import com.durganmcbroom.resources.ResourceNotFoundException
import com.durganmcbroom.resources.toByteArray
import com.fasterxml.jackson.module.kotlin.readValue
import com.kaolinmc.boot.util.basicObjectMapper
import com.kaolinmc.integration.neoforge.metadata.NeoConfig

class NeoForgeArtifactRepository :
    ArtifactRepository<SimpleMavenRepositorySettings, NeoForgeArtifactRequest, NeoForgeArtifactMetadata> {
    override val factory: RepositoryFactory<SimpleMavenRepositorySettings, ArtifactRepository<SimpleMavenRepositorySettings, NeoForgeArtifactRequest, NeoForgeArtifactMetadata>>
        get() = NeoForge
    override val name: String = "neoforge"
    override val settings: SimpleMavenRepositorySettings = neoforgeRepository

    override suspend fun get(request: NeoForgeArtifactRequest): NeoForgeArtifactMetadata {
        try {
            val resource = settings.layout.resourceOf(
                "net.neoforged",
                "neoforge",
                request.descriptor.version,
                "moddev-config",
                "json"
            )

            val config = basicObjectMapper.readValue<NeoConfig>(resource.open().toByteArray())

            return NeoForgeArtifactMetadata(
                request.descriptor,
                listOf(),
                config.copy(
                    libraries = config.libraries + setOf(
                        "org.slf4j:slf4j-api:2.0.16"
                    )
                )
            )
        } catch (e: ResourceNotFoundException) {
            throw MetadataRequestException.MetadataNotFound(request.descriptor, "config.json", e)
        }
    }
}