package com.kaolinmc.integration.neoforge.resolver

import com.durganmcbroom.artifact.resolver.ArtifactRepository
import com.durganmcbroom.artifact.resolver.RepositoryFactory
import com.durganmcbroom.artifact.resolver.simple.maven.*
import com.durganmcbroom.resources.Resource
import com.kaolinmc.archives.ArchiveHandle
import com.kaolinmc.boot.archive.*
import com.kaolinmc.boot.dependency.DependencyNode
import com.kaolinmc.boot.dependency.DependencyResolver
import com.kaolinmc.boot.maven.MavenLikeResolver
import com.kaolinmc.boot.monad.Either
import com.kaolinmc.boot.monad.Tree
import com.kaolinmc.boot.util.mapAsync
import com.kaolinmc.common.util.resolve
import kotlinx.coroutines.awaitAll
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path

class NeoForgeLibraryResolver(
    parentClassLoader: ClassLoader
) : DependencyResolver<SimpleMavenDescriptor, SimpleMavenArtifactRequest, NeoForgeLibraryNode, SimpleMavenRepositorySettings, SimpleMavenArtifactMetadata>(
    parentClassLoader
), MavenLikeResolver<NeoForgeLibraryNode, SimpleMavenArtifactMetadata> {
    override val id: String = "neoforge-libraries"
    override val metadataType: Class<SimpleMavenArtifactMetadata> = SimpleMavenArtifactMetadata::class.java
    override val factory: RepositoryFactory<SimpleMavenRepositorySettings, ArtifactRepository<SimpleMavenRepositorySettings, SimpleMavenArtifactRequest, SimpleMavenArtifactMetadata>>
        get() = SimpleMaven

    override fun pathForDescriptor(descriptor: SimpleMavenDescriptor, classifier: String, type: String): Path {
        return Path(
            descriptor.group.replace('.', File.separatorChar),
            descriptor.artifact,
            descriptor.version,
            descriptor.classifier ?: "",
            "${descriptor.artifact}-${descriptor.version}.$type"
        )
    }

    override suspend fun cache(
        metadata: SimpleMavenArtifactMetadata,
        parents: List<Tree<Either<SimpleMavenArtifactMetadata, TaggedIArchive>>>,
        helper: CacheHelper<SimpleMavenDescriptor>
    ): Tree<TaggedIArchive> {
        helper.withResource("jar.jar", metadata.resource())

        return helper.newData(
            metadata.descriptor,
            parents.mapAsync {
                helper.cache(
                    it, this,
                )
            }.awaitAll()
        )
    }

    override fun load(
        data: ArchiveData<SimpleMavenDescriptor, CachedArchiveResource>,
        accessTree: ArchiveAccessTree,
        helper: ResolutionHelper
    ): NeoForgeLibraryNode {
        return NeoForgeLibraryNode(
            data.resources["jar.jar"]!!.path,
            accessTree,
            data.descriptor
        )
    }

    override fun constructNode(
        descriptor: SimpleMavenDescriptor,
        handle: ArchiveHandle?,
        parents: Set<NeoForgeLibraryNode>,
        accessTree: ArchiveAccessTree
    ): NeoForgeLibraryNode {
        throw UnsupportedOperationException()
    }

    override suspend fun SimpleMavenArtifactMetadata.resource(): Resource? = jar()
}

data class NeoForgeLibraryNode(
    val path: Path?,
    override val access: ArchiveAccessTree,
    override val descriptor: SimpleMavenDescriptor,
) : DependencyNode<SimpleMavenDescriptor>() {
    override val handle: ArchiveHandle? = null
}