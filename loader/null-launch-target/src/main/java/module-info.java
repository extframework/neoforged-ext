import com.kaolinmc.integration.neoforge.launch.NullLaunchTarget;
import cpw.mods.modlauncher.api.ILaunchHandlerService;

module kaolinmc.forge.launch {
    requires fml_loader;
    requires kotlin.stdlib;
    requires cpw.mods.securejarhandler;

    exports com.kaolinmc.integration.neoforge.launch;

    provides ILaunchHandlerService with NullLaunchTarget;
}