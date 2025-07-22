package com.kaolinmc.integration.neoforge

import com.durganmcbroom.artifact.resolver.ArtifactMetadata
import com.durganmcbroom.artifact.resolver.simple.maven.SimpleMavenDescriptor
import com.kaolinmc.archives.ArchiveHandle
import com.kaolinmc.archives.zip.ZipFinder
import com.kaolinmc.archives.zip.ZipReference
import com.kaolinmc.archives.zip.classLoaderToArchive
import com.kaolinmc.boot.archive.ArchiveAccessTree
import com.kaolinmc.boot.archive.ArchiveTarget
import com.kaolinmc.boot.archive.ClassLoadedArchiveNode
import com.kaolinmc.boot.loader.ArchiveResourceProvider
import com.kaolinmc.boot.loader.ArchiveSourceProvider
import com.kaolinmc.boot.loader.MutableClassLoader
import com.kaolinmc.boot.loader.MutableResourceProvider
import com.kaolinmc.boot.loader.MutableSourceProvider
import com.kaolinmc.boot.loader.ResourceProvider
import com.kaolinmc.common.util.resolve
import com.kaolinmc.core.app.api.ApplicationDescriptor
import com.kaolinmc.core.minecraft.api.MappingNamespace
import com.kaolinmc.core.minecraft.api.MinecraftApp
import com.kaolinmc.core.minecraft.internal.MojangMappingProvider
import java.net.URL
import java.nio.file.Path
import java.util.jar.JarFile
import java.util.zip.ZipFile
import kotlin.io.path.Path
import kotlin.text.removePrefix

class NeoForgeMinecraftImpl(
    override val gameDir: Path,
    classpath: List<Path>,
    override val mainClass: String,
    override val version: String,
) : MinecraftApp {
    override val gameJar: Path =
        gameDir resolve Path(
            "libraries",
            "net",
            "neoforged",
            "neoforge",
            "21.7.23-beta",
            "neoforge-21.7.23-beta-client.jar"
        )
    override var classpath: List<Path> = classpath + listOf(
        gameJar,
        gameDir resolve Path(
            "libraries",
            "net",
            "minecraft",
            "client",
            "1.21.7-20250711.194848",
            "client-1.21.7-20250711.194848-extra.jar"
        )
    )

    override val namespace: MappingNamespace
        get() = MojangMappingProvider.Companion.DEOBF_TYPE
    override val path: Path = gameDir

    override val node: ClassLoadedArchiveNode<ApplicationDescriptor> =
        object : ClassLoadedArchiveNode<ApplicationDescriptor> {
            override val descriptor: ApplicationDescriptor = com.kaolinmc.core.app.api.ApplicationDescriptor(
                "net.minecraft",
                "client",
                version,
                null
            )

            override val access: ArchiveAccessTree = object : ArchiveAccessTree {
                override val descriptor: ArtifactMetadata.Descriptor = com.kaolinmc.core.app.api.ApplicationDescriptor(
                    "net.minecraft",
                    "client",
                    version,
                    null
                )
                override val targets: List<ArchiveTarget> = listOf()
            }

            private val references by lazy {
                this@NeoForgeMinecraftImpl.classpath.map { it ->
                    JarFile(
                        it.toFile(),
                        true,
                        ZipFile.OPEN_READ,
                        Runtime.version()
                    ) to it.toUri()
                }
            }

            override val handle: ArchiveHandle = classLoaderToArchive(
                MutableClassLoader(
                    name = "neoforged-minecraft",
                    resources = MutableResourceProvider(
                        references.mapTo(ArrayList()) { (reference, location) ->
                            object : ResourceProvider {
                                override fun findResources(name: String): Sequence<URL> {
                                    return reference.getJarEntry(name)?.let {
                                        URL("jar:${location}!/${it.realName.removePrefix("/")}")
                                    }?.let { sequenceOf(it) } ?: emptySequence()
                                }
                            }
                        }
                    ),
                    sources = MutableSourceProvider(
                        references.mapTo(ArrayList()) {
                            ArchiveSourceProvider(
                                ZipReference(
                                    it.first,
                                    it.second
                                )
                            )
                        }
                    ),
                    parent = ClassLoader.getSystemClassLoader(),
                )
            )
        }

    // TODO wow this is hacky
    fun confirmClassPath(
        exclude: List<SimpleMavenDescriptor>
    ) {
        classpath = classpath.filterNot {
            val list = it.toList()

            exclude.any { desc ->
                val artifact = list[list.size - 3].toString()

                desc.artifact == artifact && list.subList(0, list.size - 3).map { it.toString() }.containsAll(
                    desc.group.split(".")
                )
            }
        }
    }
}