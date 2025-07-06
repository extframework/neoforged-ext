package com.kaolinmc.extension.neoforge.mapping

import com.kaolinmc.common.util.resolve
import com.kaolinmc.core.minecraft.environment.mappingProvidersAttrKey
import com.kaolinmc.tooling.api.environment.ExtensionEnvironment
import com.kaolinmc.tooling.api.environment.wrkDirAttrKey
import com.kaolinmc.tooling.api.tweaker.EnvironmentTweaker

class NeoforgeMappingTweaker : EnvironmentTweaker {
    override fun tweak(environment: ExtensionEnvironment) {
        environment[mappingProvidersAttrKey].add(
            McpLegacyMappingProvider(environment[wrkDirAttrKey].value resolve "mappings")
        )
    }
}