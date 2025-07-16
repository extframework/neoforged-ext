package com.kaolinmc.integration.neoforge.resolver

import com.durganmcbroom.artifact.resolver.ArtifactRepository
import com.durganmcbroom.artifact.resolver.RepositoryFactory
import com.durganmcbroom.artifact.resolver.simple.maven.SimpleMavenArtifactRequest
import com.durganmcbroom.artifact.resolver.simple.maven.SimpleMavenDescriptor
import com.durganmcbroom.artifact.resolver.simple.maven.SimpleMavenRepositorySettings
import com.durganmcbroom.resources.Resource
import com.kaolinmc.archives.ArchiveHandle
import com.kaolinmc.boot.archive.ArchiveAccessTree
import com.kaolinmc.boot.archive.ArchiveData
import com.kaolinmc.boot.archive.ArchiveException
import com.kaolinmc.boot.archive.ArchiveTrace
import com.kaolinmc.boot.archive.CacheHelper
import com.kaolinmc.boot.archive.CachedArchiveResource
import com.kaolinmc.boot.archive.ResolutionHelper
import com.kaolinmc.boot.archive.TaggedIArchive
import com.kaolinmc.boot.dependency.DependencyResolver
import com.kaolinmc.boot.monad.Either
import com.kaolinmc.boot.monad.Tree
import com.kaolinmc.boot.util.mapAsync
import com.kaolinmc.boot.util.requireKeyInDescriptor
import com.kaolinmc.integration.neoforge.artifact.NeoForge
import com.kaolinmc.integration.neoforge.artifact.NeoForgeArtifactMetadata
import com.kaolinmc.integration.neoforge.artifact.NeoForgeArtifactRequest
import com.kaolinmc.integration.neoforge.artifact.NeoForgeDescriptor
import com.kaolinmc.integration.neoforge.artifact.neoforgeRepository
import kotlinx.coroutines.awaitAll
import java.lang.module.ModuleFinder
import java.nio.file.Path
import kotlin.io.path.Path

class NeoForgeResolver(
    val libraryResolver: NeoForgeLibraryResolver,
    parentClassLoader: ClassLoader
) : DependencyResolver<
        NeoForgeDescriptor,
        NeoForgeArtifactRequest,
        NeoForgeLoaderNode,
        SimpleMavenRepositorySettings,
        NeoForgeArtifactMetadata
        >(parentClassLoader) {

    override fun constructNode(
        descriptor: NeoForgeDescriptor,
        handle: ArchiveHandle?,
        parents: Set<NeoForgeLoaderNode>,
        accessTree: ArchiveAccessTree
    ): NeoForgeLoaderNode = throw UnsupportedOperationException()

    override suspend fun NeoForgeArtifactMetadata.resource(): Resource? {
        return null
    }

    override val id: String = "neoforge"
    override val metadataType: Class<NeoForgeArtifactMetadata> = NeoForgeArtifactMetadata::class.java
    override val factory: RepositoryFactory<SimpleMavenRepositorySettings, ArtifactRepository<SimpleMavenRepositorySettings, NeoForgeArtifactRequest, NeoForgeArtifactMetadata>>
        get() = NeoForge

    override fun serializeDescriptor(descriptor: NeoForgeDescriptor): Map<String, String> {
        return mapOf(
            "version" to descriptor.version,
        )
    }

    override fun deserializeDescriptor(
        descriptor: Map<String, String>,
        trace: ArchiveTrace
    ): NeoForgeDescriptor = NeoForgeDescriptor(
        descriptor.requireKeyInDescriptor("version") { trace }
    )

    override fun pathForDescriptor(
        descriptor: NeoForgeDescriptor,
        classifier: String,
        type: String
    ): Path {
        return Path("neoforge", descriptor.version, "$classifier.$type")
    }

    override fun load(
        data: ArchiveData<NeoForgeDescriptor, CachedArchiveResource>,
        accessTree: ArchiveAccessTree,
        helper: ResolutionHelper
    ): NeoForgeLoaderNode {
        val dependencyPaths = accessTree.targets
            .map { it.relationship.node }
            .filterIsInstance<NeoForgeLibraryNode>()
            .mapNotNull { it.path }

        val finder = ModuleFinder.of(*dependencyPaths.toTypedArray())
        val roots = finder.findAll().map {
            it.descriptor().name()
        }

        val parent = ModuleLayer.boot()

        val config = parent.configuration().resolve(finder, ModuleFinder.of(), roots)

        val layer = parent.defineModulesWithOneLoader(config, ClassLoader.getSystemClassLoader())

        val module = layer.findModule("cpw.mods.bootstraplauncher").get()

        val handle = object : ArchiveHandle {
            override val classloader: ClassLoader = module.classLoader
            override val name: String = module.name
            override val packages: Set<String> = module.packages
            override val parents: Set<ArchiveHandle> = setOf()
        }

        return NeoForgeLoaderNode(
            handle,
            data.descriptor,
            accessTree
        )
    }

    override suspend fun cache(
        metadata: NeoForgeArtifactMetadata,
        parents: List<Tree<Either<NeoForgeArtifactMetadata, TaggedIArchive>>>,
        helper: CacheHelper<NeoForgeDescriptor>
    ): Tree<TaggedIArchive> {
        val dependencies = metadata.config.libraries.mapNotNull {
            val descriptor = SimpleMavenDescriptor.parseDescription(it)!!
            val request = SimpleMavenArtifactRequest(descriptor, false)

            try {
                helper.cache(
                    request,
                    neoforgeRepository,
                    libraryResolver
                )
            } catch (e: ArchiveException.ArchiveNotFound) {
                try {
                    helper.cache(
                        request,
                        SimpleMavenRepositorySettings.default(
                            "https://libraries.minecraft.net"
                        ),
                        libraryResolver
                    )
                } catch (e: ArchiveException.ArchiveNotFound) {
                    helper.cache(
                        request,
                        SimpleMavenRepositorySettings.mavenCentral(),
                        libraryResolver
                    )
                }
            }
        }

        return helper.newData(
            metadata.descriptor,
            dependencies//.awaitAll()
        )
    }
}