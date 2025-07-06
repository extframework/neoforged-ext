package com.kaolinmc.extension.neoforge.mapping

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.kaolinmc.archive.mapper.ArchiveMapping
import com.kaolinmc.archive.mapper.MappingsProvider
import com.kaolinmc.archive.mapper.parsers.mcp.MCPMappingParser
import com.kaolinmc.archive.mapper.parsers.mcp.MCPMappingResolver
import com.kaolinmc.common.util.make
import com.kaolinmc.common.util.resolve
import java.io.FileOutputStream
import java.net.URL
import java.net.URLConnection
import java.nio.file.Path

class McpLegacyMappingProvider(
    private val path: Path
) : MappingsProvider {
    private companion object {
        private const val VERSIONS_URL = "https://maven.minecraftforge.net/de/oceanlabs/mcp/versions.json"
    }

    private val parser = MCPMappingParser("mojang:obfuscated", "mcp-legacy:deobfuscated")

    override val namespaces: Set<String> = setOf(parser.srcNamespace, parser.targetNamespace)

    override fun forIdentifier(identifier: String): ArchiveMapping {
        val (channel, mcpId) = fetchMcpVersion(identifier)
            ?: throw IllegalArgumentException("Unknown minecraft version: '$identifier'. There are no MCP mappings for this version.")

        val mappingsPath = MCPMappingResolver.resolve(
            path resolve "mcp-legacy_$identifier",
            identifier,
            channel, mcpId
        )

        val mappingsIn = mappingsPath.toFile().inputStream()
        return parser.parse(mappingsIn)
    }

    // Fetches channel, version id
    internal fun fetchMcpVersion(
        minecraftVersion: String
    ): Pair<String, String>? {
        val versionsPath = path resolve "mcp-legacy_versions.json"
        if (versionsPath.make()) {
            val connection: URLConnection = URL(VERSIONS_URL).openConnection()
            connection.addRequestProperty("User-Agent", "kaolin")

            connection.getInputStream().use { versionsIn ->
                FileOutputStream(versionsPath.toFile()).use { versionsOut ->
                    val buffer = ByteArray(1024)
                    var len: Int
                    while ((versionsIn.read(buffer).also { len = it }) != -1) {
                        versionsOut.write(buffer, 0, len)
                    }
                }
            }
        }

        val versions: Map<String, Map<String, List<Int>>> = jacksonObjectMapper().readValue<Map<String, Map<String, List<Int>>>>(versionsPath.toFile())

        val channels: Map<String, List<Int>> = versions[minecraftVersion] ?: return null

        return channels["stable"]?.maxOrNull()?.let { "stable" to it.toString() }
            ?: channels["snapshot"]?.maxOrNull()?.let { "stable" to it.toString() }
    }
}

