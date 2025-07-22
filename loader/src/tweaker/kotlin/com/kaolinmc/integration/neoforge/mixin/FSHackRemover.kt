package com.kaolinmc.integration.neoforge.mixin

import com.kaolinmc.mixin.api.InjectCode
import com.kaolinmc.mixin.api.Mixin
import com.kaolinmc.mixin.api.MixinFlow
import cpw.mods.niofs.union.UnionFileSystem

@Mixin(UnionFileSystem::class)
class FSHackRemover {
    @InjectCode(
        "<clinit>"
    )
    fun removeLookupCall(
        flow: MixinFlow
    ) : MixinFlow.Result<*> {
        return flow.yield()
    }

    @InjectCode(
        "bindToLayer"
    )
    fun removeBindCall(
        flow: MixinFlow
    ) : MixinFlow.Result<*> {
        return flow.yield()
    }
}