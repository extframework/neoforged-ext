package dev.extframework.extension.neoforge.mapping

import dev.extframework.common.util.resolve
import dev.extframework.core.minecraft.environment.mappingProvidersAttrKey
import dev.extframework.tooling.api.environment.ExtensionEnvironment
import dev.extframework.tooling.api.environment.wrkDirAttrKey
import dev.extframework.tooling.api.tweaker.EnvironmentTweaker

class NeoforgeMappingTweaker : EnvironmentTweaker {
    override fun tweak(environment: ExtensionEnvironment) {
        environment[mappingProvidersAttrKey].add(
            McpLegacyMappingProvider(environment[wrkDirAttrKey].value resolve "mappings")
        )
    }
}