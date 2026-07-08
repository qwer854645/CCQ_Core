package ccq.core.compat.coe;

import ccq.core.CcqCoreMod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;

public final class CoeCompat {
    private static boolean enabled;

    private CoeCompat() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(CoeNetwork::register);

        enabled = ModList.get().isLoaded("createoreexcavation") && ModList.get().isLoaded("create");
        if (!enabled) {
            CcqCoreMod.LOGGER.info("Create Ore Excavation not loaded; skipping ore vein scanner.");
            return;
        }

        CoeItems.ITEMS.register(modEventBus);
        CoeMenus.MENUS.register(modEventBus);
        CcqCoreMod.LOGGER.info("Create Ore Excavation compatibility enabled.");
    }
}
