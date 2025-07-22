package com.kaolinmc.integration.neoforge.resolver

import com.durganmcbroom.artifact.resolver.ArtifactRepository
import com.durganmcbroom.artifact.resolver.RepositoryFactory
import com.durganmcbroom.artifact.resolver.simple.maven.SimpleMavenArtifactRequest
import com.durganmcbroom.artifact.resolver.simple.maven.SimpleMavenDescriptor
import com.durganmcbroom.artifact.resolver.simple.maven.SimpleMavenRepositorySettings
import com.durganmcbroom.resources.Resource
import com.kaolinmc.archives.ArchiveHandle
import com.kaolinmc.archives.zip.ZipFinder
import com.kaolinmc.boot.archive.*
import com.kaolinmc.boot.dependency.DependencyResolver
import com.kaolinmc.boot.loader.ArchiveSourceProvider
import com.kaolinmc.boot.loader.DelegatingSourceProvider
import com.kaolinmc.boot.loader.packages
import com.kaolinmc.boot.monad.Either
import com.kaolinmc.boot.monad.Tree
import com.kaolinmc.boot.util.requireKeyInDescriptor
import com.kaolinmc.boot.util.toEnumeration
import com.kaolinmc.common.util.open
import com.kaolinmc.common.util.readInputStream
import com.kaolinmc.integration.neoforge.NeoForgeTweaker
import com.kaolinmc.integration.neoforge.NeoForgeTweaker.Companion.neoforgeController
import com.kaolinmc.integration.neoforge.artifact.*
import com.kaolinmc.mixin.MixinEngine
import java.lang.module.Configuration
import java.lang.module.ModuleFinder
import java.net.URL
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.CodeSource
import java.security.ProtectionDomain
import java.security.cert.Certificate
import java.util.Enumeration
import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.jar.Manifest
import kotlin.io.path.Path
import kotlin.jvm.optionals.getOrElse
import kotlin.streams.asSequence

class NeoForgeResolver(
    val libraryResolver: NeoForgeLibraryResolver,
    parentClassLoader: ClassLoader,
    val engine: MixinEngine,
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
        return Path(
            "net",
            "neoforged",
            "neoforge",
            descriptor.version,
            "neoforge-${descriptor.version}-${classifier}.$type"
        )
    }

    override fun load(
        data: ArchiveData<NeoForgeDescriptor, CachedArchiveResource>,
        accessTree: ArchiveAccessTree,
        helper: ResolutionHelper
    ): NeoForgeLoaderNode {
//        val launchTargetPath = Files.createTempDirectory("launch")
        val launchJar = data.resources["launch_target.jar"]!!.path
//        val coreJar = data.resources["universal.jar"]!!.path

        val dependencyPaths = accessTree.targets
            .map { it.relationship.node }
            .filterIsInstance<NeoForgeLibraryNode>()
            .filterNot { it.descriptor.artifact == "earlydisplay" }
            .filterNot { it.descriptor.artifact == "mixinextras-neoforge" }
            .mapNotNull { it.path } + listOf(launchJar)

//        val archives = dependencyPaths.map {
//            ZipFinder.find(it)
//        }

//        val delegate = DelegatingSourceProvider(archives.map(::ArchiveSourceProvider))

        val finder = ModuleFinder.of(*dependencyPaths.toTypedArray())
        val all = finder.findAll()

        val loader = object : ClassLoader("NeoForge", NeoForgeTweaker.minecraft.node.handle!!.classloader) {
            init {
                registerAsParallelCapable()
            }

            override fun findResources(name: String): Enumeration<URL> {
                return all.mapNotNull { it.open().find(name).orElseGet { null } }
                    .map { it.toURL() }
                    .asSequence().toEnumeration()
            }

            override fun findResource(name: String): URL? {
                return findResources(name).toList().firstOrNull()
            }

            override fun findResource(moduleName: String, name: String): URL? {
                return all.find { it.descriptor().name() == moduleName }
                    ?.open()?.find(name)?.orElseGet { null }?.toURL()
            }

            override fun findClass(moduleName: String?, name: String): Class<*>? {
                return this.findClass(name)
            }

            override fun findClass(name: String): Class<*>? {
                val injectedClass = when (name) {
                    "cpw.mods.cl.ModuleClassLoader" -> "/ModuleClassLoader.class"
                    "cpw.mods.cl.ProtectionDomainHelper" -> "/ProtectionDomainHelper.class"
                    "cpw.mods.niofs.union.UnionFileSystem" -> "/UnionFileSystem.class"
                    "net.neoforged.fml.loading.FMLLoader" -> "/FMLLoader.class"
//                        "cpw.mods.jarhandling.impl.JarContentsImpl" -> "/JarContentsImpl.class"
//                        "cpw.mods.niofs.union.UnionFileSystemProvider" -> "/UnionFileSystemProvider.class"
//                        "cpw.mods.jarhandling.VirtualJar" -> "/VirtualJar.class"
                    else -> null
                }

                val packageName = name.substringBeforeLast(".")

                val resourceName = name.replace('.', '/') + ".class"

                val module = all.find {
                    it.open().find(resourceName).isPresent
                } ?: return null

                val content = injectedClass
                    ?.let(this::class.java::getResourceAsStream)
                    ?.use {
                        it.readInputStream()
                            .let(ByteBuffer::wrap)
                    } ?: module.open().read(resourceName).get()

                if (getDefinedPackage(packageName) == null) {
                    val manifest = module.open()
                        .find(JarFile.MANIFEST_NAME)
                        .orElseGet { null }
                        ?.open()
                        ?.let(::Manifest)

                    if (manifest != null) {
                        val attr = manifest.getAttributes(packageName) ?: manifest.mainAttributes

                        definePackage(
                            packageName,
                            attr.getValue(Attributes.Name.SPECIFICATION_TITLE),
                            attr.getValue(Attributes.Name.SPECIFICATION_VERSION),
                            attr.getValue(Attributes.Name.SPECIFICATION_VENDOR),
                            attr.getValue(Attributes.Name.IMPLEMENTATION_TITLE),
                            attr.getValue(Attributes.Name.IMPLEMENTATION_VERSION),
                            attr.getValue(Attributes.Name.IMPLEMENTATION_VENDOR),
                            null
                        )
                    }
                }

                val cls = defineClass(
                    name, content, ProtectionDomain(
                        CodeSource(module.location().get().toURL(), arrayOf<Certificate>()),
                        null
                    )
                )

                return cls
//                return injectedClass
//                    ?.let(this::class.java::getResourceAsStream)
//                    ?.use {
//                        it
//                            .readInputStream()
//                            .let(ByteBuffer::wrap)
//                    } ?: delegate.findSource(name)

//                    val source = delegate.findSource(name) ?: return null

//                    val node = ClassNode()
//                    val reader = ClassReader(source.toBytes())
//                    reader.accept(node, ClassReader.EXPAND_FRAMES)
//
//                    engine.transform(node)
//
//                    val bytes = AwareClassWriter(archives, 0, reader).also {
//                        node.accept(it)
//                    }.toByteArray()

//                    return ByteBuffer.wrap(bytes)
            }
        }


        val roots = all.map {
            it.descriptor().name()
        }

        val parent = ModuleLayer.boot()

        val config: Configuration = parent.configuration().resolve(finder, ModuleFinder.of(), roots)

        val controller = ModuleLayer.defineModules(config, listOf(parent), { name: String -> loader })
        neoforgeController = controller

        val handle = object : ArchiveHandle {
            override val classloader: ClassLoader = loader
            override val name: String = data.descriptor.name
            override val packages: Set<String> = all.flatMapTo(HashSet()) {
                it.open().list().asSequence()
                    .mapTo(HashSet()) {
                        it.removeSuffix(".class")
                            .substringBeforeLast('/')
                    }.map { it.replace('/', '.') }
            }
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
        helper.withResource(
            "launch_target.jar",
            Resource(javaClass.getResource("/null-launch-target.jar")!!.toURI().toString()) {
                javaClass.getResourceAsStream("/null-launch-target.jar")!!
            })

        helper.withResource(
            "universal.jar",
            metadata.core
        )

        val dependencies = metadata.config.libraries.map {
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