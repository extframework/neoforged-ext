package com.kaolinmc.integration.neoforge

import com.kaolinmc.integration.neoforge.resolver.NeoForgeLibraryResolver
import com.kaolinmc.integration.neoforge.resolver.NeoForgeLoaderProvider
import com.kaolinmc.integration.neoforge.resolver.NeoForgeResolver
import com.kaolinmc.tooling.api.ExtensionLoader
import com.kaolinmc.tooling.api.environment.ExtensionEnvironment
import com.kaolinmc.tooling.api.environment.dependencyTypesAttrKey
import com.kaolinmc.tooling.api.tweaker.EnvironmentTweaker

class NeoForgeTweaker : EnvironmentTweaker {
    override fun tweak(environment: ExtensionEnvironment) {
        val libraryResolver = NeoForgeLibraryResolver(ClassLoader.getSystemClassLoader())
        val resolver = NeoForgeResolver(libraryResolver, ClassLoader.getSystemClassLoader())

        val graphResolvers = environment[ExtensionLoader].graph.resolvers
        graphResolvers.register(libraryResolver)
        graphResolvers.register(resolver)

        environment[dependencyTypesAttrKey].container.register(
            NeoForgeLoaderProvider(
                resolver
            )
        )
    }
}