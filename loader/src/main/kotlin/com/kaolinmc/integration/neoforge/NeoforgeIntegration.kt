package com.kaolinmc.integration.neoforge

import com.kaolinmc.archives.transform.AwareClassWriter
import com.kaolinmc.archives.zip.ZipFinder
import com.kaolinmc.boot.loader.ClassProvider
import com.kaolinmc.boot.loader.ResourceProvider
import com.kaolinmc.boot.loader.packages
import com.kaolinmc.common.util.runCatching
import com.kaolinmc.core.entrypoint.Entrypoint
import com.kaolinmc.core.minecraft.util.emptyArchiveReference
import com.kaolinmc.core.minecraft.util.write
import com.kaolinmc.integration.neoforge.resolver.NeoForgeLibraryNode
import com.kaolinmc.tooling.api.extension.artifact.ExtensionDescriptor
import cpw.mods.bootstraplauncher.BootstrapLauncher
import cpw.mods.cl.ModuleClassLoader
import cpw.mods.modlauncher.Launcher
import cpw.mods.modlauncher.TransformingClassLoader
import cpw.mods.modlauncher.api.IModuleLayerManager
import cpw.mods.niofs.union.UnionFileSystemProvider
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import java.net.URL
import java.nio.file.Files
import java.nio.file.spi.FileSystemProvider
import java.util.concurrent.ConcurrentHashMap

class NeoforgeIntegration : Entrypoint() {
    override fun init() {
        runInstaller()

        (NeoForgeTweaker.minecraft as? NeoForgeMinecraftImpl)?.confirmClassPath(
            NeoForgeTweaker.graph.nodes.map { it.value.value }
                .filterIsInstance<NeoForgeLibraryNode>()
                .map { it.descriptor }
        )

        // Setup empty jar (will become Minecraft) for the null launch target
        val emptyJar = Files.createTempFile("empty", ".jar")
        emptyArchiveReference().write(emptyJar)
        System.setProperty("neoforge.tweaker.empty", emptyJar.toString())

        // Needed by FML
        System.setProperty(
            "legacyClassPath",
            ""
        )

//        System.setProperty(
//            "neoforge.tweaker.game.paths",
//            (NeoForgeTweaker.minecraft.classpath + listOf(NeoForgeTweaker.minecraft.gameJar))
//                .joinToString(separator = File.pathSeparator)
//        )

        // Setup secure jar fs provider
        FileSystemProvider.installedProviders()
        val field = FileSystemProvider::class.java
            .getDeclaredField("installedProviders")
            .apply { isAccessible = true }

        val content = field.get(null) as MutableList<FileSystemProvider>
        val newContent =
            content + UnionFileSystemProvider() + net.neoforged.jarjar.nio.layzip.LayeredZipFileSystemProvider()
        field.set(null, newContent)
        NeoForgeTweaker.neoforgeController.addOpens(
            BootstrapLauncher::class.java.module,
            "cpw.mods.bootstraplauncher",
            javaClass.module
        )

        // Setup context class loader (not for service loading, for parent class loaders)
        Thread.currentThread().contextClassLoader = BootstrapLauncher::class.java.classLoader

        // Minecraft launcher supplied properties
        System.setProperty("libraryDirectory", NeoForgeTweaker.graph.path.toString())

        // Launch
        NeoForgeTweaker.neoforgeController.addExports(
            module("org.spongepowered.mixin"),
            "org.spongepowered.asm.mixin.transformer",
            module("fml_loader")
        )

        val runMethod = BootstrapLauncher::class.java.getDeclaredMethod(
            "run",
            Boolean::class.java,
            Array<String>::class.java
        ).apply { trySetAccessible() }

        val args = arrayOf(
            "--fml.neoForgeVersion",
            "21.7.23-beta",
            "--fml.fmlVersion",
            "9.0.14",
            "--fml.mcVersion",
            "1.21.7",
            "--fml.neoFormVersion",
            "20250711.194848",
            "--launchTarget",
            "kaolin-integration",
        )

        // Wont actually launch
        runMethod.invoke(
            null,
            false,
            args
        )

        // Allowing reflecting into NeoForge
        NeoForgeTweaker.neoforgeController.openAll(
            Launcher::class.java.module,
            javaClass.module,
            "cpw.mods.modlauncher",
        )
        NeoForgeTweaker.neoforgeController.openAll(
            module("cpw.mods.securejarhandler"),
            javaClass.module,
            "cpw.mods.cl",
        )

        // Mixin setup
        val transformingLoader = Launcher::class.java.getDeclaredField(
            "classLoader"
        ).apply { trySetAccessible() }.get(Launcher.INSTANCE) as TransformingClassLoader

        transformingLoader.setFallbackClassLoader(
            BootstrapLauncher::class.java.classLoader,
        )

        // Patch in Mixin extras access config
        val layerHandler = Launcher::class.java.getDeclaredField(
            "moduleLayerHandler"
        ).apply { trySetAccessible() }.get(Launcher.INSTANCE) as IModuleLayerManager

        val mixinExtras = layerHandler.getLayer(IModuleLayerManager.Layer.GAME).get().findModule(
            "mixinextras.neoforge"
        ).get()
        val neoforge = layerHandler.getLayer(IModuleLayerManager.Layer.GAME).get().findModule(
            "neoforge"
        ).get()
        NeoForgeTweaker.neoforgeController.openAll(
            module("org.spongepowered.mixin"),
            mixinExtras,
            "org.spongepowered.asm.mixin.transformer",
            "org.spongepowered.asm.mixin.transformer.ext",
            "org.spongepowered.asm.mixin.injection.struct",
            "org.spongepowered.asm.mixin.transformer.ext.extensions",
            "org.spongepowered.asm.mixin.injection.modify"
        )
        NeoForgeTweaker.neoforgeController.openAll(
            module("org.spongepowered.mixin"),
            neoforge,
            "org.spongepowered.include.com.google.common.base",
        )

        // Open Minecraft / App classes to NeoForge
        val parents = ModuleClassLoader::class.java.getDeclaredField("parentLoaders")
            .apply { trySetAccessible() }.get(transformingLoader) as MutableMap<String, ClassLoader>

        val allPackages = (NeoForgeTweaker.minecraft.classpath)
            .toSet().map { ZipFinder.find(it) }
            .flatMapTo(HashSet()) { it.packages }

        parents.putAll(
            allPackages.associateWith {
                // Replace with target linker
//                NeoForgeTweaker.linker.targetLoader
                NeoForgeTweaker.linker.target.node.handle!!.classloader
            }
        )

        // Open NeoForge classes to Minecraft
        NeoForgeTweaker.linker.extensionClasses[ExtensionDescriptor(
            "net.neoforged", "neoforge", "1"
        )] = object : ClassProvider {
            override val packages: Set<String> = setOf()

            override fun findClass(name: String): Class<*>? = runCatching(ClassNotFoundException::class) {
                if (!allPackages.contains(name.substringBeforeLast("."))) {
                    layerHandler.getLayer(IModuleLayerManager.Layer.GAME).get()
                        .modules().first().classLoader.loadClass(name)
                } else null
            }
        }

        NeoForgeTweaker.linker.extensionResources[ExtensionDescriptor(
            "net.neoforged", "neoforge", "1"
        )] = object : ResourceProvider {
            override fun findResources(name: String): Sequence<URL> {
                return layerHandler.getLayer(IModuleLayerManager.Layer.GAME).get()
                    .modules().first().classLoader.getResources(name).asSequence()
            }
        }

//        val classTransformer = ClassTransformer::class.java.declaredConstructors.first {
//            it.parameterCount == 3
//        }.apply { trySetAccessible() }.newInstance(
//            Launcher::class.java.getDeclaredField("transformStore")
//                .apply { trySetAccessible() }
//                .get(Launcher.INSTANCE),
//            Launcher::class.java.getDeclaredField("launchPlugins")
//                .apply { trySetAccessible() }
//                .get(Launcher.INSTANCE),
//            null
//        )

        // Get class transformer for mixin operations
        val classTransformer = transformingLoader::class.java.getDeclaredField("classTransformer")
            .apply { trySetAccessible() }.get(transformingLoader)

        // TODO this is a hacky way to prevent reentrance in sponge/mixin
        val threadSecurity = ConcurrentHashMap<Thread, Unit>()

        NeoForgeTweaker.mixinDelegateAgent = agent@{ name, node ->
            if (threadSecurity.put(Thread.currentThread(), Unit) != null) {
                return@agent node
            }

            var node = node
            val target = NeoForgeTweaker.minecraft
            // Let knot transform the bytes, want to do this so it can read
            // it into node form with whatever flags it prefers.

            val writer = AwareClassWriter(
                listOfNotNull(target.node.handle),
                0
            )
            node?.accept(writer)
            val bytes = if (node == null) byteArrayOf() else writer.toByteArray()

            try {
                val transformed = classTransformer::class.java.getDeclaredMethod(
                    "transform",
                    ByteArray::class.java,
                    String::class.java, String::class.java
                )
                    .apply { trySetAccessible() }
                    .invoke(classTransformer, bytes, name, "Mixin")
                        as? ByteArray ?: return@agent node

                val reader = ClassReader(transformed)
                val transformedNode = ClassNode()
                reader.accept(transformedNode, ClassReader.EXPAND_FRAMES)

                node = transformedNode
            } catch (e: Throwable) {
                e.printStackTrace()
            }

            threadSecurity.remove(Thread.currentThread())

            node
        }


//        Thread.currentThread().contextClassLoader = NeoForgeTweaker.minecraft.node.handle!!.classloader
    }

    private fun ModuleLayer.Controller.openAll(
        source: Module,
        target: Module,
        vararg packages: String
    ) {
        for (pn in packages) {
            addOpens(
                source,
                pn,
                target
            )
        }
    }

    private fun module(name: String): Module {
        return NeoForgeTweaker.neoforgeController.layer().findModule(name).get()
    }
}