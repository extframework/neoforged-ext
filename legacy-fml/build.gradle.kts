import dev.extframework.gradle.common.dm.jobs
import dev.extframework.gradle.common.toolingApi

extension {
    partitions {
        tweaker {
            dependencies {
                toolingApi()
                jobs()
            }
        }
    }
}