package ccq.core.compat.storage;

import ccq.core.CcqCoreMod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;

public final class StorageCompat {
    private StorageCompat() {
    }

    public static boolean isEnabled() {
        return ModList.get().isLoaded("fxntstorage") && ModList.get().isLoaded("functionalstorage");
    }

    public static void register(IEventBus modEventBus) {
        if (!isEnabled()) {
            CcqCoreMod.LOGGER.info("FXNT Storage or Functional Storage missing — skipping storage bridge");
            return;
        }
        StorageBridgeBlocks.register(modEventBus);
        modEventBus.addListener(StorageBridgeCapabilities::register);
        CcqCoreMod.LOGGER.info("Registered FXNT ↔ Functional Storage bridge");
    }
}
