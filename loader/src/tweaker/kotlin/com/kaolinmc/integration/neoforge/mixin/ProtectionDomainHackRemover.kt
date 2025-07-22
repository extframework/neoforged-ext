package com.kaolinmc.integration.neoforge.mixin

import com.kaolinmc.mixin.api.InjectCode
import com.kaolinmc.mixin.api.Mixin
import com.kaolinmc.mixin.api.MixinFlow

@Mixin(cpw.mods.cl.ProtectionDomainHelper::class)
class ProtectionDomainHackRemover {
    @InjectCode(
        "<clinit>"
    )
    fun removeLookupCall(
        flow: MixinFlow
    ) : MixinFlow.Result<*> {
        return flow.yield()
    }
}