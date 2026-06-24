package ccq.core.compat.cmr;

import ccq.core.CcqCoreMod;
import fr.iglee42.cmr.cooler.SnowmanCoolerBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class CmrSnowmanCapabilities {
    private CmrSnowmanCapabilities() {
    }

    public static void register(IEventBus modEventBus) {
        if (!CmrFixes.isEnabled()) {
            return;
        }
        modEventBus.addListener(CmrSnowmanCapabilities::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        BuiltInRegistries.BLOCK_ENTITY_TYPE.getOptional(SnowmanCoolerItems.CMR_COOLER_BE).ifPresent(type ->
                event.registerBlockEntity(
                        Capabilities.ItemHandler.BLOCK,
                        type,
                        (be, side) -> new SnowmanCoolerFuelStorage((SnowmanCoolerBlockEntity) be)
                )
        );

        CcqCoreMod.LOGGER.info("Registered snowman cooler item handler capability for Jade");
    }
}
