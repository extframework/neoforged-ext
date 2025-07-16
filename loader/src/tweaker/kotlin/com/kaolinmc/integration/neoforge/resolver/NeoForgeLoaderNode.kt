package com.kaolinmc.integration.neoforge.resolver

import com.kaolinmc.archives.ArchiveHandle
import com.kaolinmc.boot.archive.ArchiveAccessTree
import com.kaolinmc.boot.dependency.DependencyNode
import com.kaolinmc.integration.neoforge.artifact.NeoForgeDescriptor

class NeoForgeLoaderNode(
    override val handle: ArchiveHandle,
    override val descriptor: NeoForgeDescriptor,
    override val access: ArchiveAccessTree
) : DependencyNode<NeoForgeDescriptor>()