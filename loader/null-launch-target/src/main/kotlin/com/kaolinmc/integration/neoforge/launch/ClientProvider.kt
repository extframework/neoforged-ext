package com.kaolinmc.integration.neoforge.launch

import com.electronwill.nightconfig.core.Config
import cpw.mods.jarhandling.JarContents
import cpw.mods.jarhandling.SecureJar
import net.neoforged.fml.ModLoadingException
import net.neoforged.fml.ModLoadingIssue
import net.neoforged.fml.loading.FMLLoader
import net.neoforged.fml.loading.LibraryFinder
import net.neoforged.fml.loading.MavenCoordinate
import net.neoforged.fml.loading.moddiscovery.ModFile
import net.neoforged.fml.loading.moddiscovery.ModFileInfo
import net.neoforged.fml.loading.moddiscovery.ModJarMetadata
import net.neoforged.fml.loading.moddiscovery.NightConfigWrapper
import net.neoforged.fml.loading.moddiscovery.locators.ProductionClientProvider
import net.neoforged.neoforgespi.ILaunchContext
import net.neoforged.neoforgespi.language.IModFileInfo
import net.neoforged.neoforgespi.locating.IDiscoveryPipeline
import net.neoforged.neoforgespi.locating.IModFile
import net.neoforged.neoforgespi.locating.IModFileCandidateLocator
import net.neoforged.neoforgespi.locating.IOrderedProvider
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path

class ClientProvider : IModFileCandidateLocator {
    override fun findCandidates(context: ILaunchContext?, pipeline: IDiscoveryPipeline) {
        val vers = FMLLoader.versionInfo()

//        val content = ArrayList<Path?>()
//        addRequiredLibrary(
//            MavenCoordinate(
//                "net.minecraft",
//                "client",
//                "",
//                "srg",
//                vers.mcAndNeoFormVersion()
//            ), content
//        )
//        addRequiredLibrary(
//            MavenCoordinate(
//                "net.minecraft",
//                "client",
//                "",
//                "extra",
//                vers.mcAndNeoFormVersion()
//            ), content
//        )

//        for (artifact in additionalContent) {
//            addRequiredLibrary(artifact, content)
//        }

        try {
            val mcJarContents = JarContents.of(
                Path(System.getProperty("neoforge.tweaker.empty"))
            )

            val mcJarMetadata = ModJarMetadata(mcJarContents)
            val mcSecureJar = SecureJar.from(mcJarContents, mcJarMetadata)
            val mcjar = IModFile.create(
                mcSecureJar
            ) { iModFile: IModFile? -> buildMinecraftModInfo(iModFile) }
            mcJarMetadata.setModFile(mcjar)

            pipeline.addModFile(mcjar)
        } catch (e: Exception) {
            pipeline.addIssue(ModLoadingIssue.error("fml.modloadingissue.corrupted_installation").withCause(e))
        }
    }

    override fun toString(): String {
        val result = StringBuilder("production client provider")
        return result.toString()
    }

    override fun getPriority(): Int {
        return IOrderedProvider.HIGHEST_SYSTEM_PRIORITY
    }

    companion object {
        private fun addRequiredLibrary(coordinate: MavenCoordinate, content: MutableList<Path?>) {
            val path = LibraryFinder.findPathForMaven(
                coordinate
            )

            if (!Files.exists(path)) {
                throw ModLoadingException(
                    ModLoadingIssue.error("fml.modloadingissue.corrupted_installation").withAffectedPath(path)
                )
            } else {
                content.add(path)
            }
        }

        fun buildMinecraftModInfo(iModFile: IModFile?): IModFileInfo {
            val modFile = iModFile as ModFile?

            // We haven't changed this in years, and I can't be asked right now to special case this one file in the path.
            val conf = Config.inMemory()
            conf.set<Any?>("modLoader", "minecraft")
            conf.set<Any?>("loaderVersion", "1")
            conf.set<Any?>("license", "All Rights Reserved")
            val mods = Config.inMemory()
            mods.set<Any?>("modId", "minecraft")
            mods.set<Any?>("version", FMLLoader.versionInfo().mcVersion())
            mods.set<Any?>("displayName", "Minecraft")
            mods.set<Any?>("authors", "Mojang Studios")
            mods.set<Any?>("description", "")
            conf.set<Any?>("mods", listOf(mods))

            val configWrapper = NightConfigWrapper(conf)
            return ModFileInfo(
                modFile,
                configWrapper,
                { file: IModFileInfo? -> configWrapper.setFile(file) },
                listOf()
            )
        }
    }
}
