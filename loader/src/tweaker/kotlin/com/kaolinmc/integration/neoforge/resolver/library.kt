package com.kaolinmc.integration.neoforge.resolver

import com.durganmcbroom.artifact.resolver.ArtifactRepository
import com.durganmcbroom.artifact.resolver.RepositoryFactory
import com.durganmcbroom.artifact.resolver.simple.maven.SimpleMaven
import com.durganmcbroom.artifact.resolver.simple.maven.SimpleMavenArtifactMetadata
import com.durganmcbroom.artifact.resolver.simple.maven.SimpleMavenArtifactRequest
import com.durganmcbroom.artifact.resolver.simple.maven.SimpleMavenDescriptor
import com.durganmcbroom.artifact.resolver.simple.maven.SimpleMavenRepositorySettings
import com.durganmcbroom.resources.LocalResource
import com.durganmcbroom.resources.Resource
import com.kaolinmc.archives.ArchiveHandle
import com.kaolinmc.archives.zip.ZipFinder
import com.kaolinmc.boot.archive.ArchiveAccessTree
import com.kaolinmc.boot.archive.ArchiveData
import com.kaolinmc.boot.archive.ArchiveNode
import com.kaolinmc.boot.archive.CacheHelper
import com.kaolinmc.boot.archive.CachedArchiveResource
import com.kaolinmc.boot.archive.ResolutionHelper
import com.kaolinmc.boot.archive.TaggedIArchive
import com.kaolinmc.boot.archive.withResource
import com.kaolinmc.boot.dependency.BasicDependencyNode
import com.kaolinmc.boot.dependency.DependencyNode
import com.kaolinmc.boot.dependency.DependencyResolver
import com.kaolinmc.boot.maven.MavenLikeResolver
import com.kaolinmc.boot.monad.Either
import com.kaolinmc.boot.monad.Tree
import com.kaolinmc.boot.util.mapAsync
import com.kaolinmc.common.util.copyTo
import com.kaolinmc.common.util.resolve
import com.kaolinmc.core.minecraft.util.write
import kotlinx.coroutines.awaitAll
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
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
        return Path("neoforge", "libraries") resolve Path(
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
        helper.withResource("jar.jar", transformModule(metadata.resource()))

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

    private suspend fun transformModule(resource: Resource?): Resource? {
        if (resource == null) return null

        val temp = Files.createTempFile(
            resource.location.substringAfterLast("/"),
            ".jar"
        )
        resource copyTo temp

        val bytes = ZipFinder.find(temp).use { archive ->
            archive.reader.entries()
                .filter { it.name.endsWith("module-info.class") }
                .toList()
                .forEach {
                    it.open().use { input ->
                        val node = ClassNode()
                        val reader = ClassReader(input)
                        reader.accept(node, 0)

                        node.module.requires.forEach { req ->
                            if (req.module != "java.base") {
                                req.access = Opcodes.ACC_STATIC// (req.access or Opcodes.ACC_STATIC_PHASE)
                            }
                        }

                        val writer = ClassWriter(reader, 0)
                        node.accept(writer)

                        val bytes = writer.toByteArray()

                        archive.writer.put(it.copy {
                            ByteArrayInputStream(bytes)
                        })
                    }
                }

            archive.write()
        }

        return Resource(resource.location) {
            ByteArrayInputStream(bytes)
        }
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