package com.kaolinmc.integration.neoforge.resolver

import com.durganmcbroom.artifact.resolver.simple.maven.SimpleMavenRepositorySettings
import com.kaolinmc.boot.dependency.DependencyResolverProvider
import com.kaolinmc.integration.neoforge.artifact.NeoForgeArtifactRequest
import com.kaolinmc.integration.neoforge.artifact.NeoForgeDescriptor
import com.kaolinmc.integration.neoforge.artifact.neoforgeRepository

class NeoForgeLoaderProvider(
    override val resolver: NeoForgeResolver,
) : DependencyResolverProvider<NeoForgeDescriptor, NeoForgeArtifactRequest, SimpleMavenRepositorySettings> {
    override val id: String by resolver::id

    override fun parseRequest(request: Map<String, String>): NeoForgeArtifactRequest? {
        val version = request["neoforge-version"] ?: return null

        return NeoForgeArtifactRequest(
            NeoForgeDescriptor(
                version
            )
        )
    }

    override fun parseSettings(settings: Map<String, String>): SimpleMavenRepositorySettings? {
        return neoforgeRepository
    }
}