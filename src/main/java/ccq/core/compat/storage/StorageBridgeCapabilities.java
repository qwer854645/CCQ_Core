package ccq.core.compat.storage;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class StorageBridgeCapabilities {
    private StorageBridgeCapabilities() {
    }

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                StorageBridgeBlocks.STORAGE_BRIDGE_BE.get(),
                (be, side) -> be.getExposedItemHandler()
        );
    }
}
