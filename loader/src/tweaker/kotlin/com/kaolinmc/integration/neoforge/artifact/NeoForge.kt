package com.kaolinmc.integration.neoforge.artifact

import com.durganmcbroom.artifact.resolver.RepositoryFactory
import com.durganmcbroom.artifact.resolver.simple.maven.SimpleMavenArtifactRequest
import com.durganmcbroom.artifact.resolver.simple.maven.SimpleMavenRepositorySettings

object NeoForge : RepositoryFactory<SimpleMavenRepositorySettings, NeoForgeArtifactRepository> {
    override fun createNew(settings: SimpleMavenRepositorySettings): NeoForgeArtifactRepository {
        return NeoForgeArtifactRepository()
    }
}