package com.kaolinmc.integration.neoforge

import com.kaolinmc.boot.archive.ArchiveGraph
import com.kaolinmc.core.app.TargetLinker
import com.kaolinmc.core.instrument.InstrumentAgent
import com.kaolinmc.core.instrument.instrumentAgentsAttrKey
import com.kaolinmc.core.instrument.internal.InstrumentedAppImpl
import com.kaolinmc.core.minecraft.InternalMinecraftApp
import com.kaolinmc.core.minecraft.api.MinecraftApp
import com.kaolinmc.core.minecraft.environment.minecraft
import com.kaolinmc.integration.neoforge.mixin.registerMixins
import com.kaolinmc.integration.neoforge.resolver.NeoForgeLibraryResolver
import com.kaolinmc.integration.neoforge.resolver.NeoForgeLoaderProvider
import com.kaolinmc.integration.neoforge.resolver.NeoForgeResolver
import com.kaolinmc.tooling.api.ExtensionLoader
import com.kaolinmc.tooling.api.environment.ExtensionEnvironment
import com.kaolinmc.tooling.api.environment.dependencyTypesAttrKey
import com.kaolinmc.tooling.api.tweaker.EnvironmentTweaker
import org.objectweb.asm.tree.ClassNode

class NeoForgeTweaker : EnvironmentTweaker {
    override fun tweak(environment: ExtensionEnvironment) {
        val libraryResolver = NeoForgeLibraryResolver(ClassLoader.getSystemClassLoader())
        val resolver = NeoForgeResolver(
            libraryResolver,
            ClassLoader.getSystemClassLoader(),
            registerMixins()
        )

        val graphResolvers = environment[ExtensionLoader].graph.resolvers
        graphResolvers.register(libraryResolver)
        graphResolvers.register(resolver)

        environment[dependencyTypesAttrKey].container.register(
            NeoForgeLoaderProvider(
                resolver
            )
        )
        graph = environment[ExtensionLoader].graph
        linker = environment[TargetLinker]

        environment[instrumentAgentsAttrKey].add(0, object : InstrumentAgent {
            override fun transformClass(
                name: String,
                node: ClassNode?
            ): ClassNode? = mixinDelegateAgent.invoke(name, node)
        })
        Companion.environment = environment

        val delegateApp = (environment.minecraft as InternalMinecraftApp).delegate
        val neoForgeApp = NeoForgeMinecraftImpl(
            delegateApp.gameDir,
            delegateApp.classpath - listOf(delegateApp.gameJar),
            delegateApp.mainClass,
            delegateApp.version,
        )

        val instrumentedApp = InstrumentedAppImpl(neoForgeApp, environment[TargetLinker], environment[instrumentAgentsAttrKey])

        environment[TargetLinker].target = instrumentedApp

        environment += instrumentedApp

        minecraft = environment.minecraft
    }

    companion object {
        lateinit var graph: ArchiveGraph
            private set
        lateinit var neoforgeController: ModuleLayer.Controller
            internal set
        lateinit var mixinDelegateAgent: ((name: String, node: ClassNode?) -> ClassNode?)
        lateinit var minecraft: MinecraftApp
            private set
        lateinit var linker: TargetLinker
            private set
        lateinit var environment: ExtensionEnvironment
    }
}