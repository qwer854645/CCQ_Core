package ccq.core.tacz;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.FMLEnvironment;

import java.nio.file.Path;

public final class CcqGunPackBootstrap {
    private CcqGunPackBootstrap() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(EventPriority.HIGHEST, CcqGunPackBootstrap::onCommonSetup);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(EventPriority.HIGHEST, CcqGunPackBootstrap::onClientSetup);
        }
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        ensureGunPacks();
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        // Safety net: TaCZ/GunsmithLib scan tacz/ during client init, before the first resource reload.
        ensureGunPacks();
    }

    static void ensureGunPacks() {
        if (!ModList.get().isLoaded("tacz")) {
            return;
        }

        var version = ModList.get().getModContainerById("tacz").orElseThrow().getModInfo().getVersion();
        if (version.getMajorVersion() == 1 && version.getMinorVersion() == 2) return;
        if(version.getMajorVersion()>1 || version.getMinorVersion()>2)return;

        Path taczDir = FMLPaths.GAMEDIR.get().resolve("tacz");
        CcqDefaultGunPackPatch.applyIfPresent(taczDir);
        AppliedArmorerPatch.applyIfPresent(taczDir);
    }
    public static void beforeOfficialPackScan() {
        if (!ModList.get().isLoaded("tacz"))return;
        var version=ModList.get().getModContainerById("tacz").orElseThrow().getModInfo().getVersion();
        if(version.getMajorVersion()!=1||version.getMinorVersion()!=2)return;
        try {
            org.slf4j.LoggerFactory.getLogger("ccq_core").info("Preparing official TACZ packs before resource scan");
            OfficialGunPackPatch.apply(FMLPaths.GAMEDIR.get().resolve("tacz"),ModList.get().getModFileById("tacz").getFile().getFilePath());
        }catch(java.io.IOException|com.google.gson.JsonParseException e){
            org.slf4j.LoggerFactory.getLogger("ccq_core").error("Official TACZ pre-scan patch failed",e);
        }
    }

}
