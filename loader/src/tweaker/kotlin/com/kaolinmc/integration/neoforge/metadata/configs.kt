package com.kaolinmc.integration.neoforge.metadata

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class NeoConfig(
    val spec: Int,
    val mcp: String,
    val ats: String,
    val binpatches: String,
    val binpatcher: Binpatcher,
    val patches: String,
    val sources: String,
    val universal: String,
    val libraries: List<String>,
    val testLibraries: List<String>,
    val runs: Map<String, RunConfig>,
    val modules: List<String>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Binpatcher(
    val version: String,
    val args: List<String>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RunConfig(
    val singleInstance: Boolean,
    val main: String,
    val args: List<String>,
    val jvmArgs: List<String>,
    val client: Boolean,
    val server: Boolean,
    val dataGenerator: Boolean,
    val gameTest: Boolean,
    val unitTest: Boolean,
    val env: Map<String, String>,
    val props: Map<String, String>
)