package com.kaolinmc.integration.neoforge.mixin

import com.kaolinmc.integration.neoforge.NeoForgeTweaker
import com.kaolinmc.mixin.MixinEngine
import com.kaolinmc.mixin.RedefinitionFlags
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode

fun registerMixins() : MixinEngine {
    val engine = MixinEngine(
        RedefinitionFlags.ONLY_INSTRUCTIONS
    )

    val mixins = listOf(
        "/com/kaolinmc/integration/neoforge/mixin/ModuleHackRemover.class",
        "/com/kaolinmc/integration/neoforge/mixin/FSHackRemover.class",
        "/com/kaolinmc/integration/neoforge/mixin/ProtectionDomainHackRemover.class"
    ).map {
        NeoForgeTweaker::class.java.getResourceAsStream(it).use {
            val node = ClassNode()
            val reader = ClassReader(it)
            reader.accept(node, ClassReader.EXPAND_FRAMES)
            node
        }
    }

    for (node in mixins) {
        engine.registerMixin(node)
    }

    return engine
}