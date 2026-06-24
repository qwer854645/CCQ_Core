package ccq.core.resourcepack;

import ccq.core.CcqCoreMod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.fml.loading.FMLEnvironment;

public final class CcaResourcePackBootstrap {
    public static final String CCA_MOD_ID = "createaddition";
    private static final ResourceLocation PACK_LOCATION = ResourceLocation.fromNamespaceAndPath(
            CcqCoreMod.MOD_ID,
            "resourcepacks/dinars_crafts_additions"
    );

    private CcaResourcePackBootstrap() {
    }

    public static void register(IEventBus modEventBus) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(CcaResourcePackBootstrap::onAddPackFinders);
        }
    }

    private static void onAddPackFinders(AddPackFindersEvent event) {
        if (!ModList.get().isLoaded(CCA_MOD_ID)) {
            return;
        }
        if (event.getPackType() != PackType.CLIENT_RESOURCES) {
            return;
        }

        event.addPackFinders(
                PACK_LOCATION,
                PackType.CLIENT_RESOURCES,
                Component.translatable("resourcepack.ccq_core.dinars_crafts_additions"),
                PackSource.BUILT_IN,
                true,
                Pack.Position.TOP
        );
        CcqCoreMod.LOGGER.info("Create Crafts & Additions detected — auto-enabling Dinar's Crafts Additions resource pack");
    }
}
