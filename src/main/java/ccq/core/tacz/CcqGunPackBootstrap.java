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

        Path taczDir = FMLPaths.GAMEDIR.get().resolve("tacz");
        CcqDefaultGunPackPatch.applyIfPresent(taczDir);
        AppliedArmorerPatch.applyIfPresent(taczDir);
    }
}
