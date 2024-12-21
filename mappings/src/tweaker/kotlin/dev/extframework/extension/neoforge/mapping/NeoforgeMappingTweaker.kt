package dev.extframework.extension.neoforge.mapping

import com.durganmcbroom.jobs.Job
import com.durganmcbroom.jobs.job
import com.durganmcbroom.jobs.logging.info
import com.durganmcbroom.jobs.logging.logger
import dev.extframework.common.util.resolve
import dev.extframework.extension.core.minecraft.environment.mappingProvidersAttrKey
import dev.extframework.tooling.api.environment.ExtensionEnvironment
import dev.extframework.tooling.api.environment.extract
import dev.extframework.tooling.api.environment.wrkDirAttrKey
import dev.extframework.tooling.api.tweaker.EnvironmentTweaker

class NeoforgeMappingTweaker : EnvironmentTweaker {
    override fun tweak(environment: ExtensionEnvironment): Job<Unit> = job {
        environment[mappingProvidersAttrKey].extract().add(
            McpLegacyMappingProvider(environment[wrkDirAttrKey].extract().value resolve "mappings")
        )
    }
}