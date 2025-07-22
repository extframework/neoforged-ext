package com.kaolinmc.integration.neoforge.launch

import cpw.mods.modlauncher.api.ServiceRunner
import net.neoforged.api.distmarker.Dist
import net.neoforged.fml.loading.FMLLoader
import net.neoforged.fml.loading.LibraryFinder
import net.neoforged.fml.loading.MavenCoordinate
import net.neoforged.fml.loading.VersionInfo
import net.neoforged.fml.loading.moddiscovery.locators.PathBasedLocator
import net.neoforged.fml.loading.moddiscovery.locators.ProductionClientProvider
import net.neoforged.fml.loading.targets.CommonLaunchHandler
import net.neoforged.neoforgespi.locating.IModFileCandidateLocator
import java.util.List
import java.util.function.Consumer

class NullLaunchTarget : CommonLaunchHandler() {
    override fun name(): String {
        return "kaolin-integration"
    }

    override fun getDist(): Dist? {
        return Dist.CLIENT
    }

    override fun isProduction(): Boolean {
        return true
    }

    override fun launchService(
        arguments: Array<out String?>?,
        gameLayer: ModuleLayer?
    ): ServiceRunner? {
        FMLLoader.beforeStart(gameLayer)
        return ServiceRunner.NOOP
    }

    override fun collectAdditionalModFileLocators(
        versionInfo: VersionInfo,
        output: Consumer<IModFileCandidateLocator>
    ) {
//        val additionalContent =
//            listOf(MavenCoordinate("net.neoforged", "neoforge", "", "client", versionInfo!!.neoForgeVersion()))
        output.accept(ClientProvider())

        val nfJar = LibraryFinder.findPathForMaven(
            "net.neoforged",
            "neoforge",
            "",
            "universal",
            versionInfo.neoForgeVersion()
        )
        output.accept(PathBasedLocator("neoforge", nfJar))
    }

    override fun runService(arguments: Array<out String?>?, gameLayer: ModuleLayer?) {
        // Nothing
    }
}