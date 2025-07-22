package com.kaolinmc.integration.neoforge.mixin

import com.kaolinmc.mixin.api.InjectCode
import com.kaolinmc.mixin.api.Mixin
import com.kaolinmc.mixin.api.MixinFlow
import cpw.mods.cl.ModuleClassLoader

@Mixin(ModuleClassLoader::class)
object ModuleHackRemover {
    @InjectCode(
        "<clinit>"
    )
    @JvmStatic
    fun removeLookupCall(
        flow: MixinFlow
    ) : MixinFlow.Result<*> {
        return flow.yield()
    }

    @InjectCode(
        "bindToLayer"
    )
    @JvmStatic
    fun removeBindCall(
        flow: MixinFlow
    ) : MixinFlow.Result<*> {
        return flow.yield()
    }
}