package com.kaolinmc.integration.neoforge.artifact

import com.durganmcbroom.artifact.resolver.ArtifactMetadata
import com.durganmcbroom.artifact.resolver.ArtifactRequest
import com.durganmcbroom.artifact.resolver.simple.maven.SimpleMavenRepositorySettings
import com.durganmcbroom.artifact.resolver.simple.maven.layout.SimpleMavenDefaultLayout
import com.durganmcbroom.resources.Resource
import com.durganmcbroom.resources.ResourceAlgorithm
import com.kaolinmc.integration.neoforge.metadata.NeoConfig

val neoforgeRepository = SimpleMavenRepositorySettings(
    SimpleMavenDefaultLayout(
        "https://maven.neoforged.net/releases",
        ResourceAlgorithm.SHA1
    ) { _, type ->
        if (type == "pom") false else true
    },
    ResourceAlgorithm.SHA1,
    requireResourceVerification = true
)

class NeoForgeDescriptor(
    val version: String
) : ArtifactMetadata.Descriptor {
    override val name: String = "NeoForge v$version"
}
class NeoForgeArtifactRequest(
    override val descriptor: NeoForgeDescriptor
) : ArtifactRequest<NeoForgeDescriptor>


class NeoForgeArtifactMetadata(
    descriptor: NeoForgeDescriptor,
    parents: List<Nothing>,
    val config: NeoConfig,
    val core: Resource
) : ArtifactMetadata<NeoForgeDescriptor, Nothing>(
    descriptor, parents
)